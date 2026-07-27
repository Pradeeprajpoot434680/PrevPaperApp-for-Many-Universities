package com.prevpaper.university.client;

import com.prevpaper.comman.dto.StudentDTO;
import com.prevpaper.comman.dto.UserData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "USER-SERVICE",
        url = "${USER_SERVICE_URL:http://user-service:8086}"
)
public interface UserServiceClient {

    @GetMapping("/api/v1/students/department/{deptId}")
    List<StudentDTO> getStudentsByDept(@PathVariable("deptId") UUID deptId);

    @GetMapping("/api/v1/students/program/{programId}")
    List<StudentDTO> getStudentsByProgram(@PathVariable("programId") UUID programId);

    @GetMapping("/api/v1/students/program/{programId}/batch/{batchYear}")
    List<StudentDTO> getStudentsByProgramAndBatch(
            @PathVariable("programId") UUID programId,
            @PathVariable("batchYear") Integer batchYear
    );

    @GetMapping("/api/v1/students/{studentId}")
    String getStudentName(@PathVariable("studentId") UUID studentId);

    // 🟢 FIXED: Exact path matching user-service endpoint (/api/v1/users/internal/bulk-profiles)
    @PostMapping("/api/v1/users/internal/bulk-profiles")
    Map<UUID, UserData> getUsersByIds(@RequestBody List<UUID> userIds);

    @PostMapping("/api/v1/users/internal/bulk-details")
    Map<UUID, StudentDTO> getBulkUserDetails(@RequestBody List<UUID> userIds);
}