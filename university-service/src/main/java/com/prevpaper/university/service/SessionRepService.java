package com.prevpaper.university.service;

import com.prevpaper.university.dtos.SubjectRequest;
import com.prevpaper.university.entities.Subject;

public interface SessionRepService {
    Subject addSubjectToSession(SubjectRequest request);
}
