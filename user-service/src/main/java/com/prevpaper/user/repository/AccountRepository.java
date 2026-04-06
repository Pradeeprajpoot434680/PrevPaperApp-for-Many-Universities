package com.prevpaper.user.repository;


import com.prevpaper.user.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserId(UUID userId);


    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.departmentId = :deptId")
    List<Account> findByDepartmentIdWithProfile(@Param("deptId") UUID deptId);

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.programId = :programId")
    List<Account> findByProgramIdWithProfile(@Param("programId") UUID programId);
}
