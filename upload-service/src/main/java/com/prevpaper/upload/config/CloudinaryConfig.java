package com.prevpaper.upload.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "your_cloud_name");
        config.put("api_key", "your_api_key");
        config.put("api_secret", "your_api_secret");

        return new Cloudinary(config);
    }
}