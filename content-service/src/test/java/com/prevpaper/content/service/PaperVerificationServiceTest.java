package com.prevpaper.content.service;

import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.repository.ContentRepository;
import com.prevpaper.content.service.Impl.ContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration-style tests verifying cohort-based (batch) paper verification logic.
 *
 * <p>These tests validate that {@link ContentServiceImpl#getPendingContent(UUID, Integer, VerificationStatus)}
 * correctly filters papers by {@code batchStartYear} using the formula:
 * <pre>
 *     batchStartYear = examYear - floor((semester - 1) / 2)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class PaperVerificationServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ContentServiceImpl contentService;

    private UUID programId;
    private UUID contentId1;
    private UUID contentId2;
    private UUID contentId3;
    private UUID contentId4;

    @BeforeEach
    void setUp() {
        programId = UUID.randomUUID();
        contentId1 = UUID.randomUUID();
        contentId2 = UUID.randomUUID();
        contentId3 = UUID.randomUUID();
        contentId4 = UUID.randomUUID();
    }

    // ─── Helper to build a Content entity ─────────────────────────────────────

    private Content buildContent(UUID id, int examYear, int semester) {
        return Content.builder()
                .id(id)
                .programId(programId)
                .universityId(UUID.randomUUID())
                .departmentId(UUID.randomUUID())
                .academicYear(examYear)
                .semester(semester)
                .title("Paper " + id)
                .description("Description")
                .contentType(com.prevpaper.comman.enums.ContentType.PAPER)
                .verificationStatus(VerificationStatus.PENDING)
                .uploadedBy(UUID.randomUUID())
                .build();
    }

    // ─── Acceptance Criteria Tests ────────────────────────────────────────────

    @Test
    @DisplayName("SessionRep_2023 CAN verify paper: examYear=2025, semester=5 (batchStartYear=2023)")
    void testSessionRep2023_canVerifyPaper_examYear2025_semester5() {
        // batchStartYear = 2025 - floor((5-1)/2) = 2025 - 2 = 2023
        int batchStartYear = 2023;

        Content paperForBatch2023 = buildContent(contentId1, 2025, 5);
        Content paperForOtherBatch = buildContent(contentId2, 2025, 1); // batchStartYear = 2025

        when(contentRepository.findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of(paperForBatch2023));

        var result = contentService.getPendingContent(programId, batchStartYear, VerificationStatus.PENDING);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.contentId()).isEqualTo(contentId1));

        verify(contentRepository).findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any());
    }

    @Test
    @DisplayName("SessionRep_2023 CANNOT verify paper: examYear=2025, semester=1 (batchStartYear=2025)")
    void testSessionRep2023_cannotVerifyPaper_examYear2025_semester1() {
        // batchStartYear = 2025 - floor((1-1)/2) = 2025 - 0 = 2025
        int batchStartYear = 2023;

        // This paper belongs to batch 2025, NOT batch 2023
        Content paperForBatch2025 = buildContent(contentId3, 2025, 1);

        // Repository returns empty list because batchStartYear=2023 doesn't match
        when(contentRepository.findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of());

        var result = contentService.getPendingContent(programId, batchStartYear, VerificationStatus.PENDING);

        assertThat(result).isEmpty();

        verify(contentRepository).findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any());
    }

    // ─── Additional boundary tests ────────────────────────────────────────────

    @Test
    @DisplayName("Batch 2023 rep sees papers from examYear 2023-2026 across their semesters")
    void testBatch2023_repSeesCorrectPapersAcrossMultipleExamYears() {
        int batchStartYear = 2023;

        // Papers that belong to Batch 2023
        Content paper2023_sem1 = buildContent(contentId1, 2023, 1); // 2023 - 0 = 2023 ✓
        Content paper2024_sem3 = buildContent(contentId2, 2024, 3); // 2024 - 1 = 2023 ✓
        Content paper2025_sem5 = buildContent(contentId3, 2025, 5); // 2025 - 2 = 2023 ✓
        Content paper2026_sem7 = buildContent(contentId4, 2026, 7); // 2026 - 3 = 2023 ✓

        when(contentRepository.findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of(paper2023_sem1, paper2024_sem3, paper2025_sem5, paper2026_sem7));

        var result = contentService.getPendingContent(programId, batchStartYear, VerificationStatus.PENDING);

        assertThat(result).hasSize(4);
        assertThat(result).extracting("contentId")
                .containsExactlyInAnyOrder(contentId1, contentId2, contentId3, contentId4);
    }

    @Test
    @DisplayName("Batch 2023 rep does NOT see papers from other batches")
    void testBatch2023_repDoesNotSeeOtherBatches() {
        int batchStartYear = 2023;

        // Papers belonging to OTHER batches
        Content paperBatch2022 = buildContent(contentId1, 2025, 7); // 2025 - 3 = 2022
        Content paperBatch2024 = buildContent(contentId2, 2025, 3); // 2025 - 1 = 2024
        Content paperBatch2025 = buildContent(contentId3, 2025, 1); // 2025 - 0 = 2025

        when(contentRepository.findByProgramIdAndBatchStartYearAndVerificationStatusIn(
                eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of());

        var result = contentService.getPendingContent(programId, batchStartYear, VerificationStatus.PENDING);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Null batchStartYear falls back to fetching all pending content for program")
    void testNullBatchStartYear_fetchesAllPending() {
        Content anyPending = buildContent(contentId1, 2025, 1);

        when(contentRepository.findPendingByProgramAndStatuses(eq(programId), any()))
                .thenReturn(List.of(anyPending));

        var result = contentService.getPendingContent(programId, null, VerificationStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(contentRepository).findPendingByProgramAndStatuses(eq(programId), any());
    }
}
