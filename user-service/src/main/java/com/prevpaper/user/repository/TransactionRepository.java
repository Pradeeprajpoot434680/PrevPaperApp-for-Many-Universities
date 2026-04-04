package com.prevpaper.user.repository;

import com.prevpaper.user.entity.UserPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface TransactionRepository extends JpaRepository<UserPointTransaction, UUID> {
    List<UserPointTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);
}
