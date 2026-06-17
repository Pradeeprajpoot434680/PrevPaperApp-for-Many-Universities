package com.prevpaper.university;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication(scanBasePackages = {
        "com.prevpaper.university", // Its own package
        "com.prevpaper.comman"      // The package where RoleEventProducer lives
})
@EnableDiscoveryClient
@EnableKafka
@EnableFeignClients
@EnableCaching
@CrossOrigin(origins = "http://localhost:5173")
public class UniversityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UniversityServiceApplication.class, args);
	}

}
