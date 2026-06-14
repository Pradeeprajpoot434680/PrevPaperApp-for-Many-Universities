package com.prevpaper.comman.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Injection via Constructor (cleaner and safer than field @Autowired)
    public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch object from Redis and deserialize to specified class target type
     */
    public <T> T get(String key, Class<T> entityClass) {
        try {
            Object rawValue = redisTemplate.opsForValue().get(key);
            if (rawValue == null) {
                return null;
            }
            // Safely parse JSON string back into target domain class type
            return objectMapper.readValue(rawValue.toString(), entityClass);
        } catch (Exception e) {
//            log.error("Error retrieving key [{}] from Redis: ", key, e);
            return null;
        }
    }

    /**
     * Save an object into Redis with a defined Time To Live (TTL)
     */
    public void set(String key, Object value, Long ttlSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
//            log.debug("Successfully cached key [{}] for {}s", key, ttlSeconds);
        } catch (Exception e) {
//            log.error("Error saving key [{}] to Redis: ", key, e);
        }
    }

    /**
     * Evict a key from the cache explicitly
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}