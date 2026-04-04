package com.prevpaper.auth.services;

import com.prevpaper.auth.dto.StudentsOfUniversityDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface InternalSync {
    List<StudentsOfUniversityDTO> findAllStudents(String universityId);
}
