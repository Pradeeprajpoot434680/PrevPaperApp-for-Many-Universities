package com.prevpaper.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
        "com.prevpaper.content",
        "com.prevpaper.comman",
        "com.prevpaper.common"
})
@EnableDiscoveryClient
@EnableKafka
@EnableFeignClients
@EnableJpaRepositories(basePackages = "com.prevpaper.content.repository") // Explicitly scans for your Jpa Beans
@EntityScan(basePackages = "com.prevpaper.content.entities")
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}