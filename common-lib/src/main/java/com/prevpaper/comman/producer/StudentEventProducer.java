package com.prevpaper.comman.producer;

import com.prevpaper.comman.dto.StudentRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class StudentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "student-events";

    public StudentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void emitStudentRegistration(UUID userId, UUID universityId, UUID departmentId, UUID programId) {
        StudentRegisteredEvent event = new StudentRegisteredEvent(userId, universityId, departmentId, programId);

        log.info("Emitting StudentRegistrationEvent: userId={}, uniId={}, deptId={}, progId={}",
                userId, universityId, departmentId, programId);

        kafkaTemplate.send(TOPIC, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✓ SUCCESS: Student event delivered to partition={}, offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ CRITICAL: Failed to publish student registration event to Kafka", ex);
                    }
                });
    }
}