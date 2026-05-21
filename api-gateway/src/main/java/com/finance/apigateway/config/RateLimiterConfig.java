package com.finance.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
            return Mono.just("rate:ip:" + ip);
        };
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
