package com.prevpaper.university.client;

import com.prevpaper.comman.dto.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

    @GetMapping("/api/v1/students/department/{deptId}")
    List<StudentDTO> getStudentsByDept(@PathVariable("deptId") UUID deptId);
}