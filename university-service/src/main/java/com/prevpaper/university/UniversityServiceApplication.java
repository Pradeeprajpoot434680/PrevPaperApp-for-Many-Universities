package com.prevpaper.university;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {
        "com.prevpaper.university", // Its own package
        "com.prevpaper.comman"      // The package where RoleEventProducer lives
})
@EnableDiscoveryClient
@EnableKafka
public class UniversityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UniversityServiceApplication.class, args);
	}

}
