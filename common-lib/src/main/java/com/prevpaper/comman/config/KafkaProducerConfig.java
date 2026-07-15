package com.prevpaper.comman.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value; // 🟢 ADD IMPORT
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

    // 🟢 FIXED: Reads from environment variables, falls back to localhost if running outside Docker
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        Map<String,Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // 🟢 FIXED
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 20971520);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String,Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic highPriorityTopic(){
        return TopicBuilder.name("high-priority-notifications").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic roleChangeTopic() {
        return TopicBuilder.name("role-change-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic bulkTopic() {
        return TopicBuilder.name("bulk-notifications").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic uploadTaskTopic() {
        return TopicBuilder.name("file-upload-task").partitions(3).replicas(1).build();
    }
}