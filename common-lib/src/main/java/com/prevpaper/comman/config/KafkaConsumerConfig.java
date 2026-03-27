package com.prevpaper.comman.config;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.dto.FileTaskEvent;
import com.prevpaper.comman.dto.RoleChangedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:default-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, false);

        // 1. Trust all relevant packages
        jsonDeserializer.addTrustedPackages(
                "com.prevpaper.comman.dto",
                "com.prevpaper.upload.dto",
                "com.prevpaper.content.dto",
                "*"
        );

        // 2. Enhanced Type Resolver for all topics
        jsonDeserializer.setTypeResolver((topic, data, headers) -> {
            if (topic.equals("file-upload-task")) {
                return TypeFactory.defaultInstance().constructType(FileTaskEvent.class);
            }
            // Fix: Both notification topics should map to CommonNotificationRequest
            if (topic.equals("high-priority-notifications") || topic.equals("bulk-notifications")) {
                return TypeFactory.defaultInstance().constructType(CommonNotificationRequest.class);
            }
            if (topic.equals("role-change-events")) {
                return TypeFactory.defaultInstance().constructType(RoleChangedEvent.class);
            }
            return TypeFactory.defaultInstance().constructType(Object.class);
        });

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // Use the injected groupId
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), jsonDeserializer);
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2L)));
        return factory;
    }
}