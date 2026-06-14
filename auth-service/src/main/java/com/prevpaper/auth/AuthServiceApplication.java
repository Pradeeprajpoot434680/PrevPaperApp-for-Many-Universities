package com.prevpaper.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.prevpaper.auth",    // Ensures controllers, config, and filters are scanned
        "com.prevpaper.comman",
        "com.prevpaper.common"
})
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.prevpaper.auth.repositories")
@EntityScan(basePackages = "com.prevpaper.auth.entities")


@EnableKafka
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}