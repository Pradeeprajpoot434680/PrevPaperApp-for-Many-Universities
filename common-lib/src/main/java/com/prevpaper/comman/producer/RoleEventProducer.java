package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.RoleChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class RoleEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "role-change-events";

    public RoleEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Updated to accept all three required fields for the RoleChangedEvent record.
     */
    public void emitRoleAssignment(UUID userId, Integer roleId, UUID scopeId) {
        try {
            // FIX: Ensure the constructor matches your updated record (userId, roleId, scopeId, action)
            RoleChangedEvent event = new RoleChangedEvent(userId, roleId, scopeId, "ASSIGN");

            log.info("Attempting to emit Role Assignment: User={}, Role={}, Scope={}",
                    userId, roleId, scopeId);

            // Send to Kafka using the userId as the message key for partition ordering
            kafkaTemplate.send(TOPIC, userId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully sent message to Kafka at offset: {}",
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message to Kafka: {}", ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("CRITICAL: Error during Kafka message preparation: {}", e.getMessage());
            // We catch but don't rethrow to avoid rolling back the DB transaction
            // if Kafka is temporarily down (optional, depending on your consistency needs).
        }
    }
}