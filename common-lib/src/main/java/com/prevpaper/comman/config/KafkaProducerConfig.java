package com.prevpaper.comman.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
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

    private final KafkaProperties kafkaProperties;

   private final SslBundles sslBundles;

    public KafkaProducerConfig(KafkaProperties kafkaProperties,
                            SslBundles sslBundles) {
        this.kafkaProperties = kafkaProperties;
        this.sslBundles = sslBundles;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
         Map<String, Object> config =
            new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));

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
    @Bean
    public NewTopic studentEventsTopic() {
        return TopicBuilder.name("student-events").partitions(3).replicas(1).build();
    }
}