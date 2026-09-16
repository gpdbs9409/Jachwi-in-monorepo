package com.capstone.Jachwi_inServerSpring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableRedisRepositories  //redis 사용할 수 있게 하는 어노테이션
@Configuration
public class RedisConfig {
    //@value 안될거 대비해서 resource 파일 생성함.
    @Value("${redis.host}")
    private String host;
    @Value("${redis.port}")
    private int port;

    //RedisConnectionFactory는 Redis와의 연결을 생성하고 관리하는 인터페이스
    //Spring Framework에서 Redis와의 연결을 설정하기 위해 RedisConnectionFactory를 Bean으로 등록하는 설정
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    //Redis를 사용하기 위해 RedisTemplate을 Bean으로 등록하는 설정
    //RedisTemplate은 Spring Data Redis에서 제공하는 클래스로, Redis 데이터베이스와 상호 작용할 수 있는 템플릿
    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory((redisConnectionFactory()));
        return redisTemplate;
    }
}
