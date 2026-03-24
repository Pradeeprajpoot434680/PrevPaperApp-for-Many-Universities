package com.prevpaper.university.service;

import com.prevpaper.university.dtos.SemesterRequest;
import com.prevpaper.university.dtos.SubjectRequest;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;

import java.util.UUID;

public interface SessionRepService {

    Semester addSemester( UUID programId, SemesterRequest request);

    Subject addSubject( UUID semesterId, SubjectRequest request);

}