package com.prevpaper.university.service.Impl;
import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserBatchRequest;
import com.prevpaper.comman.dto.UserDetailDTO;
import com.prevpaper.comman.enums.ScopeType;
import com.prevpaper.university.client.AuthClient;
import com.prevpaper.university.client.UserServiceClient;
import com.prevpaper.university.dtos.DepartmentRepResponse;
import com.prevpaper.university.entities.Department;
import com.prevpaper.university.entities.RepresentativeAssignment;
import com.prevpaper.university.repository.DepartmentRepository;
import com.prevpaper.university.repository.RepresentativeRepository;
import com.prevpaper.university.service.RepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class RepresentativeServiceImpl implements RepresentativeService {

    private final RepresentativeRepository representativeRepository;
    private final DepartmentRepository departmentRepo;
    private final AuthClient authClient;
    private final UserServiceClient userServiceClient;
    @Override
    public List<DepartmentRepResponse> getDeptRepsByUniversity(UUID universityId) {

        List<RepresentativeAssignment> assignments =
                representativeRepository.findByScopeTypeAndIsActiveTrue(ScopeType.DEPARTMENT);

        Map<UUID, String> deptNames = departmentRepo.findByUniversityId(universityId)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        List<RepresentativeAssignment> uniAssignments = assignments.stream()
                .filter(a -> deptNames.containsKey(a.getScopeId()))
                .toList();

        if (uniAssignments.isEmpty()) return Collections.emptyList();

        List<UUID> userIds = uniAssignments.stream()
                .map(RepresentativeAssignment::getUserId)
                .toList();

        List<UserDetailDTO> userDetails =
                authClient.getUserDetailsBatch(new UserBatchRequest(userIds));

        Map<UUID, UserDetailDTO> userMap = userDetails.stream()
                .collect(Collectors.toMap(UserDetailDTO::userId, u -> u));

        return uniAssignments.stream()
                .map(assign -> {
                    UserDetailDTO user = userMap.get(assign.getUserId());
                    return new DepartmentRepResponse(
                            user != null ? user.email() : "N/A",
                            user != null ? user.fullName() : "Unknown User",
                            deptNames.get(assign.getScopeId())
                    );
                }).toList();
    }

    @Override
    public List<StudentDTO> getStudentsByDepartment(UUID deptId) {
        // Here we just proxy the request to the User Service
        // You could add logic here to check if the Rep has access to this deptId
        return userServiceClient.getStudentsByDept(deptId);
    }
}