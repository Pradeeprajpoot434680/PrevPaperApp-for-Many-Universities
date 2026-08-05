package com.prevpaper.comman.config;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.dto.FileTaskEvent;
import com.prevpaper.comman.dto.RoleChangedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
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

    private static final String DEFAULT_GROUP_ID = "default-group";

    private final KafkaProperties kafkaProperties;
    private final SslBundles sslBundles;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties,
                               SslBundles sslBundles) {
        this.kafkaProperties = kafkaProperties;
        this.sslBundles = sslBundles;
    }
   

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

            if (topic.equals("student-events")) {
                return TypeFactory.defaultInstance().constructType(
                        com.prevpaper.comman.dto.StudentRegisteredEvent.class
                );
            }

            return TypeFactory.defaultInstance().constructType(Object.class);
        });

        Map<String,Object> config =
        new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));
        config.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_GROUP_ID);
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