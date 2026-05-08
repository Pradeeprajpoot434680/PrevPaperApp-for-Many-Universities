package com.prevpaper.content.repository;

import com.prevpaper.content.dto.ContentSearchRequest;
import com.prevpaper.content.entities.Content;
import com.prevpaper.content.enums.VerificationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class ContentSpecifications {
    public static Specification<Content> withFilters(ContentSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Context Filter (University is usually mandatory)
            if (request.getUniversityId() != null) {
                predicates.add(cb.equal(root.get("universityId"), request.getUniversityId()));
            }

            // 2. Only show VERIFIED content to the public library
            predicates.add(cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));

            // 3. Optional Hierarchical Filters
            if (request.getDepartmentId() != null)
                predicates.add(cb.equal(root.get("departmentId"), request.getDepartmentId()));

            if (request.getProgramId() != null)
                predicates.add(cb.equal(root.get("programId"), request.getProgramId()));

            if (request.getSemester() != null)
                predicates.add(cb.equal(root.get("semester"), request.getSemester()));

            if (request.getSubjectId() != null)
                predicates.add(cb.equal(root.get("subjectId"), request.getSubjectId()));

            // 4. Content and Exam Type Logic
            if (request.getContentType() != null)
                predicates.add(cb.equal(root.get("contentType"), request.getContentType()));

            if (request.getExamTypeId() != null)
                predicates.add(cb.equal(root.get("examTypeId"), request.getExamTypeId()));

            if (request.getAcademicYear() != null)
                predicates.add(cb.equal(root.get("academicYear"), request.getAcademicYear()));

            // Sort by latest created first
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}