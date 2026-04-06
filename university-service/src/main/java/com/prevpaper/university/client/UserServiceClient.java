package com.prevpaper.university.client;

import com.prevpaper.comman.dto.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

    @GetMapping("/api/v1/students/department/{deptId}")
    List<StudentDTO> getStudentsByDept(@PathVariable("deptId") UUID deptId);

    @GetMapping("/api/v1/students/{studentId}")
    String getStudentName(@PathVariable UUID studentId);

    @GetMapping("/api/v1/users/internal/bulk-details")
    Map<UUID, StudentDTO> getBulkUserDetails(@RequestBody List<UUID> userIds);

    @GetMapping("/api/v1/students/program/{programId}")
    List<StudentDTO> getStudentsByProgram(@PathVariable("programId") UUID programId);
};