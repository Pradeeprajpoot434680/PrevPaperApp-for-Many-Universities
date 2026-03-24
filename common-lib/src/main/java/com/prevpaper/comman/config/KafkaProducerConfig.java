package com.prevpaper.comman.config;

import com.prevpaper.comman.dto.CommonNotificationRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration

public class KafkaProducerConfig {

    private final String bootstrapServers = "localhost:9092";

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String,Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String,Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic highPriorityTopic(){
        return TopicBuilder.name("high-priority-notifications")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleChangeTopic() {
        return TopicBuilder.name("role-change-events")
                .partitions(3)    // 3 partitions for better scalability
                .replicas(1)      // 1 replica for local development
                .build();
    }

    @Bean
    public NewTopic bulkTopic() {
        return TopicBuilder.name("bulk-notifications")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
