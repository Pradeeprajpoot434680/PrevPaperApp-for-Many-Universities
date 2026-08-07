package com.prevpaper.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication(
        exclude = {

                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                // The gateway has no Kafka consumers/producers; without this, a KafkaAdmin
                // is auto-created (via common-lib's NewTopic beans) and endlessly tries to
                // reach the default localhost:9092 broker, spamming connection errors.
                KafkaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "com.prevpaper.gateway",
                "com.prevpaper.comman"
        },
        excludeFilters = {
                // Preserve Spring Boot's standard component-scan filters
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                // The gateway has no Kafka consumers/producers; skip common-lib's Kafka
                // configs and producers so no KafkaAdmin/NewTopic beans are created.
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                com.prevpaper.comman.config.KafkaProducerConfig.class,
                                com.prevpaper.comman.config.KafkaConsumerConfig.class,
                                com.prevpaper.comman.config.SendNotification.class
                        }),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.prevpaper\\.comman\\.producer\\..*"
                )
        }
)
@EnableDiscoveryClient
@EnableFeignClients
@CrossOrigin(origins = "*")
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
