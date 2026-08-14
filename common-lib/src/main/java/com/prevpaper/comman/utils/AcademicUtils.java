package com.prevpaper.comman.utils;

/**
 * Utility class for academic cohort/batch calculations.
 *
 * <p>Refactors SessionRep verification from exam-year-based to cohort/batch-based ownership.
 *
 * <p>Given an uploaded paper's {@code examYear} and {@code semester},
 * the responsible {@code batchStartYear} is calculated as:
 *
 * <pre>
 *     batchStartYear = examYear - floor((semester - 1) / 2)
 * </pre>
 *
 * <h3>Example Mapping Matrix:</h3>
 * <ul>
 *   <li>Exam Year: 2025, Semester: 1 or 2 → Batch 2025 Rep</li>
 *   <li>Exam Year: 2025, Semester: 3 or 4 → Batch 2024 Rep</li>
 *   <li>Exam Year: 2025, Semester: 5 or 6 → Batch 2023 Rep</li>
 *   <li>Exam Year: 2025, Semester: 7 or 8 → Batch 2022 Rep</li>
 * </ul>
 */
public final class AcademicUtils {

    private AcademicUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Calculates the batch start year for a given exam year and semester.
     *
     * <p>A SessionRep assigned to {@code batchStartYear} is responsible for verifying
     * papers uploaded by students of that cohort as they progress through semesters.
     *
     * @param examYear  the calendar year the exam was held (e.g., 2025)
     * @param semester  the semester number (1–8 for a typical 4-year program)
     * @return the batch start year that owns this paper
     * @throws IllegalArgumentException if semester is not between 1 and 12
     */
    public static int calculateBatchStartYear(int examYear, int semester) {
        if (semester < 1 || semester > 12) {
            throw new IllegalArgumentException(
                    "Invalid semester number: " + semester + ". Must be between 1 and 12."
            );
        }
        return examYear - ((semester - 1) / 2);
    }
}
