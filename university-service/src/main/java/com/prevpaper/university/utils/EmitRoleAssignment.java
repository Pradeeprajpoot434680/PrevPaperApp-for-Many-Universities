package com.prevpaper.university.utils;

import com.prevpaper.comman.producer.RoleEventProducer;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@Slf4j
public class EmitRoleAssignment {
    private  final RoleEventProducer roleEventProducer;

    public EmitRoleAssignment(RoleEventProducer roleEventProducer) {
        this.roleEventProducer = roleEventProducer;
    }

    @Transactional
    public void sendEmittedRoleAssignmentToKafka(UUID userId,Integer roleId,UUID scopeId){
        log.info("Role assignment emit registered for transaction commit: userId={}, roleId={}, scopeId={}",
                userId, roleId, scopeId);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Role assignment emit started after commit: userId={}, roleId={}, scopeId={}",
                                userId, roleId, scopeId);
                        roleEventProducer.emitRoleAssignment(userId,roleId,scopeId);
                        log.info("Role assignment emit completed after commit: userId={}, roleId={}, scopeId={}",
                                userId, roleId, scopeId);
                    }
                }
        );
    }
}
