package com.prevpaper.content.controller;

import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.content.enums.VerificationStatus;
import com.prevpaper.content.service.ContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * API-level unit tests for {@link ContentController#getPendingBySession(UUID, Integer)}.
 *
 * <p>Verifies that the controller correctly passes {@code batchStartYear} to the service layer
 * for cohort-based paper filtering.
 */
class ContentControllerApiTest {

    // ─── Helper ───────────────────────────────────────────────────────────────

    private PendingContentDTO buildPendingDto(UUID contentId, String title) {
        return new PendingContentDTO(
                contentId, title, "Description", "Uploader", "http://file.url",
                "PDF", java.time.LocalDateTime.now(), "Subject", com.prevpaper.comman.enums.ContentType.PAPER
        );
    }

    // ─── Acceptance Criteria: API returns only batch-assigned papers ───────────

    @Test
    @DisplayName("GET /api/v1/content/internal/pending/session returns only papers for the rep's batch")
    void testGetPendingBySession_returnsOnlyBatchPapers() {
        // Arrange
        UUID programId = UUID.randomUUID();
        int batchStartYear = 2023;
        ContentService contentService = Mockito.mock(ContentService.class);

        PendingContentDTO batchPaper = buildPendingDto(UUID.randomUUID(), "Batch 2023 Paper");
        Mockito.when(contentService.getPendingContent(eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of(batchPaper));

        ContentController controller = new ContentController(contentService);

        // Act
        var result = controller.getPendingBySession(programId, batchStartYear);

        // Assert
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.contentId()).isEqualTo(batchPaper.contentId()));

        Mockito.verify(contentService).getPendingContent(eq(programId), eq(batchStartYear), eq(VerificationStatus.PENDING));
    }

    @Test
    @DisplayName("GET /api/v1/content/internal/pending/session returns empty list when no papers for batch")
    void testGetPendingBySession_returnsEmptyWhenNoBatchPapers() {
        // Arrange
        UUID programId = UUID.randomUUID();
        int batchStartYear = 2023;
        ContentService contentService = Mockito.mock(ContentService.class);

        Mockito.when(contentService.getPendingContent(eq(programId), eq(batchStartYear), any()))
                .thenReturn(List.of());

        ContentController controller = new ContentController(contentService);

        // Act
        var result = controller.getPendingBySession(programId, batchStartYear);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/content/internal/pending/session uses batchStartYear param (not academicYear)")
    void testGetPendingBySession_usesBatchStartYearParam() {
        // Arrange
        UUID programId = UUID.randomUUID();
        int batchStartYear = 2023;
        ContentService contentService = Mockito.mock(ContentService.class);

        Mockito.when(contentService.getPendingContent(any(), any(), any()))
                .thenReturn(List.of());

        ContentController controller = new ContentController(contentService);

        // Act
        controller.getPendingBySession(programId, batchStartYear);

        // Assert: verify the service was called with batchStartYear=2023
        Mockito.verify(contentService)
                .getPendingContent(eq(programId), eq(2023), eq(VerificationStatus.PENDING));
    }
}
