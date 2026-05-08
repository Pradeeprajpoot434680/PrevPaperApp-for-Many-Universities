package com.prevpaper.university.service;

import com.prevpaper.comman.dto.PendingContentDTO;
import com.prevpaper.university.dtos.*;
import com.prevpaper.university.entities.Semester;
import com.prevpaper.university.entities.Subject;

import java.util.List;
import java.util.UUID;

public interface SessionRepService {

    Semester addSemester( UUID programId, SemesterRequest request);

    Subject addSubject( UUID semesterId, SubjectRequest request);

//    SessionRepDashboardDTO getSessionDashboard(UUID sessionId);

    List<SubjectResourceDTO> getSubjectsWithStats(UUID sessionId,Integer semesterNumber);
    List<SubjectResourceDTO> getSubjectsBySemesterId( UUID semesterId);

    SessionRepDashboardDTO getSessionDashboard(UUID sessionId);

    List<PendingContentDTO> getPendingContentBySession(UUID sessionId);
    void updateContentStatus(UUID contentId, UUID repId, VerifyContentRequest request);
    void deleteContent(UUID contentId);

}