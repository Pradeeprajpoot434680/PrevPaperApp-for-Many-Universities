package com.prevpaper.comman.config;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.prevpaper.comman.dto.CommonNotificationRequest;
import com.prevpaper.comman.dto.RoleChangedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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

    private final String bootstrapServers = "localhost:9092";

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        // 1. Create the deserializer for Object
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class, false);

        // 2. IMPORTANT: Add trusted packages
        deserializer.addTrustedPackages("com.prevpaper.comman.dto", "com.prevpaper.auth.dto", "*");

        // 3. Fix the "LinkedHashMap" issue by telling it to use headers for type info
        // If the producer and consumer are in the same project structure, this works:
        deserializer.setTypeResolver((topic, data, headers) -> {
            // You can manually force it for specific topics if headers are missing
            if (topic.equals("high-priority-notifications")) {
                return TypeFactory.defaultInstance().constructType(CommonNotificationRequest.class);
            }
            if (topic.equals("role-change-events")) {
                return TypeFactory.defaultInstance().constructType(RoleChangedEvent.class);
            }
            return TypeFactory.defaultInstance().constructType(Object.class);
        });

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
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