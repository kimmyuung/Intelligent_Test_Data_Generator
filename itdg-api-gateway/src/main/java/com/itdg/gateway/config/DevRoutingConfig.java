package com.itdg.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 개발 환경용 정적 라우팅 설정
 * 
 * Eureka/Consul 없이 정적 URL로 라우팅
 */
@Configuration
@Profile("dev")
public class DevRoutingConfig {

    @Value("${service.orchestrator.url:http://localhost:8080}")
    private String orchestratorUrl;

    @Value("${service.analyzer.url:http://localhost:8081}")
    private String analyzerUrl;

    @Value("${service.generator.url:http://localhost:8082}")
    private String generatorUrl;

    @Value("${service.ml-server.url:http://localhost:8000}")
    private String mlServerUrl;

    @Bean
    public RouteLocator devRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Orchestrator
                .route("orchestrator-dev", r -> r
                        .path("/api/orchestrator/**")
                        .uri(orchestratorUrl))

                // Orchestrator 스트리밍
                .route("orchestrator-stream-dev", r -> r
                        .path("/api/orchestrator/stream/**")
                        .uri(orchestratorUrl))

                // ML API (Orchestrator 프록시)
                .route("ml-api-dev", r -> r
                        .path("/api/ml/**")
                        .uri(orchestratorUrl))

                // Analyzer
                .route("analyzer-dev", r -> r
                        .path("/api/analyze/**")
                        .uri(analyzerUrl))

                // Generator
                .route("generator-dev", r -> r
                        .path("/api/generator/**")
                        .uri(generatorUrl))

                // ML Server 직접 접근 (개발용)
                .route("ml-server-direct-dev", r -> r
                        .path("/ml-direct/**")
                        .filters(f -> f.rewritePath("/ml-direct/(?<segment>.*)", "/${segment}"))
                        .uri(mlServerUrl))

                .build();
    }
}
