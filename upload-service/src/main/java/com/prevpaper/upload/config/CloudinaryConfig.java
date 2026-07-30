//package com.prevpaper.upload.config;
//
//import com.cloudinary.Cloudinary;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class CloudinaryConfig {
//
//    @Bean
//    public Cloudinary cloudinary() {
//        Map<String, String> config = new HashMap<>();
//        config.put("cloud_name", "dzuhuayg7");
//        config.put("api_key", "385695865177824");
//        config.put("api_secret", "2I7tOxJ6UeGPOOxhGgcaurqwOoA");
//
//        return new Cloudinary(config);
//    }
//}

package com.prevpaper.upload.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:your_cloud_name}")
    private String cloudName;

    @Value("${cloudinary.api-key:your_api_key}")
    private String apiKey;

    @Value("${cloudinary.api-secret:your_api_secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}