package com.prevpaper.university.service;

import com.prevpaper.university.dtos.IdNameDTO;
import com.prevpaper.university.dtos.ProgramStructureDTO;
import com.prevpaper.university.dtos.UniversityTeamMemberDTO;

import java.util.List;
import java.util.UUID;

public interface UniversityDiscoveryService {
    List<UniversityTeamMemberDTO> getUniversityTeam(UUID universityId);
    List<IdNameDTO> getDepartments(UUID universityId);
    List<IdNameDTO> getPrograms(UUID departmentId);
    ProgramStructureDTO getProgramStructure(UUID programId);
    List<IdNameDTO> getSubjects(UUID semesterId);
    List<IdNameDTO> getExamConfigs(UUID universityId);
}
