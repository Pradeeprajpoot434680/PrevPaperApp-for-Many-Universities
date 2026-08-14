package com.prevpaper.content.repository;

import com.prevpaper.comman.enums.ContentType;
import com.prevpaper.content.dto.ContentTypeCountDTO;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, JpaSpecificationExecutor<Content> {
    long countByProgramIdAndSemesterAndVerificationStatus(
            UUID programId,
            Integer semester,
            VerificationStatus status
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Content c
        WHERE c.universityId = :universityId
          AND c.departmentId = :departmentId
          AND c.programId = :programId
          AND c.semester = :semester
          AND c.subjectId = :subjectId
          AND c.contentType = :contentType
          AND c.academicYear = :academicYear
          AND (:examTypeId IS NULL OR c.examTypeId = :examTypeId)
    """)
    boolean existsDuplicateContent(
            @Param("universityId") UUID universityId,
            @Param("departmentId") UUID departmentId,
            @Param("programId") UUID programId,
            @Param("semester") Integer semester,
            @Param("subjectId") UUID subjectId,
            @Param("contentType") ContentType contentType,
            @Param("academicYear") Integer academicYear,
            @Param("examTypeId") UUID examTypeId
    );


//    @Query("SELECT new com.prevpaper.content.dto.ContentTypeCountDTO(c.contentType, COUNT(c)) " +
//            "FROM Content c " +
//            "WHERE c.universityId = :universityId AND c.verificationStatus = 'VERIFIED' " +
//            "GROUP BY c.contentType")
//    List<ContentTypeCountDTO> countContentGroupedByType(@Param("universityId") UUID universityId);


    @Query("SELECT new com.prevpaper.content.dto.ContentTypeCountDTO(c.contentType, COUNT(c)) " +
            "FROM Content c " +
            "WHERE c.universityId = :universityId " + // Removed status filter
            "GROUP BY c.contentType")
    List<ContentTypeCountDTO> countContentGroupedByType(@Param("universityId") UUID universityId);

    // Also need a total count
    long countByUniversityIdAndVerificationStatus(UUID universityId, VerificationStatus status);

    List<Content> findByProgramIdOrUniversityIdOrDepartmentId(UUID progId, UUID univId, UUID deptId);

    // Specifically for Sessions if you use academicYear as a filter
    List<Content> findByProgramIdAndAcademicYearAndVerificationStatus(
            UUID programId, Integer academicYear, VerificationStatus status
    );

    List<Content> findByVerificationStatusAndUniversityIdOrDepartmentIdOrProgramId(
            VerificationStatus status, UUID univId, UUID deptId, UUID progId);

    boolean existsByUniversityIdAndDepartmentIdAndProgramIdAndSemesterAndSubjectId(
            UUID universityId,
            UUID departmentId,
            UUID programId,
            Integer semester,
            UUID subjectId
    );


    @Query("SELECT c FROM Content c WHERE c.programId = :programId AND c.verificationStatus IN :statuses")
    List<Content> findPendingByProgramAndStatuses(
            @Param("programId") UUID programId,
            @Param("statuses") List<VerificationStatus> statuses
    );

    // Keep existing method for exact academic year matching if needed
    List<Content> findByProgramIdAndAcademicYearAndVerificationStatusIn(
            UUID programId,
            Integer academicYear,
            List<VerificationStatus> statuses
    );

    // 🟢 COHORT-BASED: SessionRep verification filter
    // Filters content where the computed batchStartYear matches the rep's assigned batch.
    // Formula: batchStartYear = academicYear - FLOOR((semester - 1) / 2)
    // This ensures a SessionRep only verifies papers from their own batch's academic progression.
    // Example: Batch 2023 rep sees papers from 2023(sem1-2), 2024(sem3-4), 2025(sem5-6), 2026(sem7-8)
    @Query("SELECT c FROM Content c WHERE c.programId = :programId " +
            "AND c.verificationStatus IN :statuses " +
            "AND (c.academicYear - FLOOR((c.semester - 1) / 2)) = :batchStartYear")
    List<Content> findByProgramIdAndBatchStartYearAndVerificationStatusIn(
            @Param("programId") UUID programId,
            @Param("batchStartYear") Integer batchStartYear,
            @Param("statuses") List<VerificationStatus> statuses
    );


}
