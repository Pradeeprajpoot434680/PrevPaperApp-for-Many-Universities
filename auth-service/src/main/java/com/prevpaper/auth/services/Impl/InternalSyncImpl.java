package com.prevpaper.auth.services.Impl;

import com.prevpaper.auth.dto.StudentsOfUniversityDTO;
import com.prevpaper.auth.entities.User;
import com.prevpaper.auth.repositories.UserRepository;
import com.prevpaper.auth.services.InternalSync;
import com.prevpaper.comman.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalSyncImpl implements InternalSync {

    private final UserRepository userRepository;

    @Override
    public List<StudentsOfUniversityDTO> findAllStudents(String universityId) {
        // 1. Convert String to UUID
        UUID uniId = UUID.fromString(universityId);

        // 2. Fetch users (filtering for ACTIVE students as an example)
        List<User> students = userRepository.findByUniversityIdAndAccountStatus(uniId, AccountStatus.ACTIVE);

        // 3. Map Entity to DTO
        return students.stream()
                .map(user -> new StudentsOfUniversityDTO(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFullName()
                ))
                .collect(Collectors.toList());
    }
}