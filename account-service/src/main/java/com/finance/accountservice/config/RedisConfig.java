package com.finance.accountservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer lettuceClientCustomizer() {
        return builder -> {
            String redisUrl = System.getenv("REDIS_URL");
            String redisSsl = System.getenv("REDIS_SSL");
            if ((redisUrl != null && redisUrl.startsWith("rediss://")) || "true".equalsIgnoreCase(redisSsl)) {
                builder.useSsl().disablePeerVerification();
            }
        };
    }

}
