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
