package com.prevpaper.university.controller;

import com.prevpaper.comman.dto.ApiResponse;
import com.prevpaper.university.dtos.SubjectRequest;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;
import com.prevpaper.university.entities.AcademicSession;
import com.prevpaper.university.service.SessionRepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/session-rep")
@RequiredArgsConstructor
public class SessionRepController {

    private final SessionRepService sessionRepService;

    @PostMapping("/subject/add")
    public ResponseEntity<ApiResponse<Subject>> addSubject(@RequestBody SubjectRequest request) {



        Subject savedSubject = sessionRepService.addSubjectToSession(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true,
                        "Subject added successfully",
                        savedSubject,
                        System.currentTimeMillis())
        );
    }
}