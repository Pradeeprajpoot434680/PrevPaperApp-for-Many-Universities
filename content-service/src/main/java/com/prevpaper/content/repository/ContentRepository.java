package com.prevpaper.content.repository;

import com.prevpaper.content.dto.ContentTypeCountDTO;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID>, JpaSpecificationExecutor<Content> {
    long countByProgramIdAndSemesterAndVerificationStatus(
            UUID programId,
            Integer semester,
            VerificationStatus status
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



}
