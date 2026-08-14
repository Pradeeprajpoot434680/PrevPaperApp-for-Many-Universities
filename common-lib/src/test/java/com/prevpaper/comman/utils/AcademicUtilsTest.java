package com.prevpaper.comman.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AcademicUtilsTest {

    // ─── Basic mapping matrix tests ───────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "2025, 1, 2025",
            "2025, 2, 2025",
            "2025, 3, 2024",
            "2025, 4, 2024",
            "2025, 5, 2023",
            "2025, 6, 2023",
            "2025, 7, 2022",
            "2025, 8, 2022",
            "2026, 8, 2023"
    })
    void testCalculateBatchStartYear(int examYear, int semester, int expectedBatchStartYear) {
        assertEquals(expectedBatchStartYear,
                AcademicUtils.calculateBatchStartYear(examYear, semester));
    }

    // ─── Specific acceptance criteria tests ───────────────────────────────────

    @Test
    void testExamYear2025_semester1_returns2025() {
        assertEquals(2025, AcademicUtils.calculateBatchStartYear(2025, 1));
    }

    @Test
    void testExamYear2025_semester2_returns2025() {
        assertEquals(2025, AcademicUtils.calculateBatchStartYear(2025, 2));
    }

    @Test
    void testExamYear2025_semester5_returns2023() {
        assertEquals(2023, AcademicUtils.calculateBatchStartYear(2025, 5));
    }

    @Test
    void testExamYear2026_semester8_returns2023() {
        assertEquals(2023, AcademicUtils.calculateBatchStartYear(2026, 8));
    }

    // ─── Boundary / edge-case tests ───────────────────────────────────────────

    @Test
    void testSemester1_alwaysReturnsExamYear() {
        assertEquals(2020, AcademicUtils.calculateBatchStartYear(2020, 1));
        assertEquals(2024, AcademicUtils.calculateBatchStartYear(2024, 1));
    }

    @Test
    void testSemester2_alwaysReturnsExamYear() {
        assertEquals(2020, AcademicUtils.calculateBatchStartYear(2020, 2));
        assertEquals(2024, AcademicUtils.calculateBatchStartYear(2024, 2));
    }

    @Test
    void testSemester3_decrementsByOne() {
        assertEquals(2023, AcademicUtils.calculateBatchStartYear(2024, 3));
    }

    @Test
    void testSemester4_decrementsByOne() {
        assertEquals(2023, AcademicUtils.calculateBatchStartYear(2024, 4));
    }

    @Test
    void testSemester5_decrementsByTwo() {
        assertEquals(2022, AcademicUtils.calculateBatchStartYear(2024, 5));
    }

    @Test
    void testSemester8_decrementsByThree() {
        assertEquals(2021, AcademicUtils.calculateBatchStartYear(2024, 8));
    }

    // ─── Invalid input tests ──────────────────────────────────────────────────

    @Test
    void testSemesterZero_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AcademicUtils.calculateBatchStartYear(2025, 0));
        assertTrue(ex.getMessage().contains("Invalid semester number"));
    }

    @Test
    void testSemesterNegative_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AcademicUtils.calculateBatchStartYear(2025, -1));
        assertTrue(ex.getMessage().contains("Invalid semester number"));
    }

    @Test
    void testSemesterAbove12_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AcademicUtils.calculateBatchStartYear(2025, 13));
        assertTrue(ex.getMessage().contains("Invalid semester number"));
    }

    @Test
    void testSemester12_isValid() {
        // Semester 12 is the upper bound and should be valid
        // 2025 - floor((12-1)/2) = 2025 - 5 = 2020
        assertEquals(2020, AcademicUtils.calculateBatchStartYear(2025, 12));
    }

    @Test
    void testSemester13_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AcademicUtils.calculateBatchStartYear(2025, 13));
        assertTrue(ex.getMessage().contains("Invalid semester number"));
    }
}
