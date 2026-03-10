package com.prevpaper.university.service.Impl;

import com.prevpaper.university.dtos.SubjectRequest;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;
import com.prevpaper.university.repository.SubjectRepository;
import com.prevpaper.university.service.SessionRepService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionRepServiceImpl implements SessionRepService {
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public Subject addSubjectToSession(SubjectRequest request) {
        Subject subject = Subject.builder()
                .name(request.getName())
                .semester(Semester.builder()
                        .id(request.getSemesterId())
                        .build())
                .build();
        return subjectRepository.save(subject);
    }
}