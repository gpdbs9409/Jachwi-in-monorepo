package com.capstone.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate template;

    public String getData(String key) {
        return template.opsForValue().get(key);
    }

    public boolean existData(String key) {
        return Boolean.TRUE.equals(template.hasKey(key));
    }

    public void deleteData(String key) {
        template.delete(key);
    }

    public void saveAuthCode(String email, String code) {
        ValueOperations<String, String> ops = template.opsForValue();
        ops.set(email, code, Duration.ofMinutes(5));
    }
}
