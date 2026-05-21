package com.finance.userservice.config;

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
            String redisHost = System.getenv("REDIS_HOST");
            String redisUrl = System.getenv("REDIS_URL");
            String redisSsl = System.getenv("REDIS_SSL");
            
            boolean isCloud = (redisHost != null && !redisHost.equals("localhost") && !redisHost.equals("127.0.0.1"))
                    || (redisUrl != null && !redisUrl.contains("localhost") && !redisUrl.contains("127.0.0.1"))
                    || "true".equalsIgnoreCase(redisSsl);
            
            if (isCloud) {
                builder.useSsl().disablePeerVerification();
            }
        };
    }


}
