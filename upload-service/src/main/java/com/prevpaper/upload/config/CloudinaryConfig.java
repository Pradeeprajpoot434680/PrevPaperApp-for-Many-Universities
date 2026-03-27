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
        config.put("cloud_name", "dzuhuayg7");
        config.put("api_key", "385695865177824");
        config.put("api_secret", "2I7tOxJ6UeGPOOxhGgcaurqwOoA");

        return new Cloudinary(config);
    }
}