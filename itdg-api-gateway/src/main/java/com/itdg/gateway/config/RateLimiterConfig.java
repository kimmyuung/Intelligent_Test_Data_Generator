package com.itdg.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter 설정
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 사용자 키 리졸버 - IP 기반
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * API 키 기반 리졸버 (헤더에서 추출)
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return Mono.just(apiKey != null ? apiKey : "anonymous");
        };
    }

    /**
     * 기본 Rate Limiter
     */
    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        // replenishRate: 초당 허용 요청 수
        // burstCapacity: 최대 버스트 요청 수
        return new RedisRateLimiter(100, 200);
    }

    /**
     * ML 전용 Rate Limiter (더 낮은 제한)
     */
    @Bean
    public RedisRateLimiter mlRateLimiter() {
        return new RedisRateLimiter(10, 20); // ML 학습은 리소스 집약적
    }
}
