package com.prevpaper.user.service.Impl;

import com.prevpaper.comman.exception.ResourceNotFoundException;
import com.prevpaper.user.entity.Account;
import com.prevpaper.user.entity.User;
import com.prevpaper.user.entity.UserPointTransaction;
import com.prevpaper.user.repository.AccountRepository;
import com.prevpaper.user.repository.TransactionRepository;
import com.prevpaper.user.repository.UserRepository;
import com.prevpaper.user.service.PointService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public Account addPoints(UUID authUserId, Integer points, String reason, String referenceId) {
        // 1. Find User by Auth ID
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        // 2. Find associated Account
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic account not found"));

        // 3. Update Balance
        account.setTotalPoints(account.getTotalPoints() + points);
        accountRepository.save(account);

        // 4. Create Audit Log
        UserPointTransaction tx = UserPointTransaction.builder()
                .account(account)
                .pointsChanged(points)
                .reason(reason)
                .referenceId(referenceId)
                .build();
        transactionRepository.save(tx);

        // 5. Emit Kafka Event (Async notification/leaderboard)
        emitPointEvent(authUserId, points, account.getTotalPoints(), reason);

        //log.info("Points added: {} to User: {}. New Balance: {}", points, authUserId, account.getTotalPoints());
        return account;
    }

    private void emitPointEvent(UUID userId, Integer added, Long total, String reason) {
        Map<String, Object> event = Map.of(
                "userId", userId,
                "pointsAdded", added,
                "newTotal", total,
                "reason", reason
        );
        kafkaTemplate.send("user-points-events", userId.toString(), event);
    }

    @Override
    public List<UserPointTransaction> getHistory(UUID authUserId) {
        User user = userRepository.findByAuthUserId(authUserId).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow();
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId());
    }
}