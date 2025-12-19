package com.itdg.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 글로벌 로깅 필터
 * 
 * 모든 요청/응답에 대한 로깅 및 트레이싱
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String START_TIME_KEY = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 요청 ID 생성 또는 기존 ID 사용
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        // 요청 시작 시간 기록
        exchange.getAttributes().put(START_TIME_KEY, Instant.now());

        // 요청 로깅
        log.info("[{}] >>> {} {} from {}",
                requestId,
                request.getMethod(),
                request.getPath(),
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress()
                        : "unknown");

        // 응답 헤더에 요청 ID 추가
        String finalRequestId = requestId;
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 응답 로깅
            ServerHttpResponse response = exchange.getResponse();
            Instant startTime = exchange.getAttribute(START_TIME_KEY);
            long duration = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : -1;

            log.info("[{}] <<< {} {} - {}ms",
                    finalRequestId,
                    response.getStatusCode(),
                    request.getPath(),
                    duration);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 가장 먼저 실행
    }
}
