package com.prevpaper.university.utils;

import com.prevpaper.comman.producer.RoleEventProducer;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class EmitRoleAssignment {
    private  final RoleEventProducer roleEventProducer;

    public EmitRoleAssignment(RoleEventProducer roleEventProducer) {
        this.roleEventProducer = roleEventProducer;
    }

    @Transactional
    public void sendEmittedRoleAssignmentToKafka(UUID userId,Integer roleId,UUID scopeId){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        roleEventProducer.emitRoleAssignment(userId,roleId,scopeId);
                    }
                }
        );
    }
}
