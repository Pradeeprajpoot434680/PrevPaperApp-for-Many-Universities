package com.prevpaper.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(
        scanBasePackages = {
                "com.prevpaper.upload",
                "com.prevpaper.comman"
        },
        // Add this exclusion line to bypass the DataSource requirement
        exclude = {

                DataSourceAutoConfiguration.class,
               HibernateJpaAutoConfiguration.class
        }
)
@EnableDiscoveryClient
@EnableFeignClients
@EnableKafka
public class UploadServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadServiceApplication.class, args);
	}

}
