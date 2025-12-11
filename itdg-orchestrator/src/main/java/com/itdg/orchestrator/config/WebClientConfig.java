package com.itdg.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${analyzer.service.url}")
    private String analyzerServiceUrl;

    @Value("${generator.service.url}")
    private String generatorServiceUrl;

    @Bean(name = "analyzerWebClient")
    public WebClient analyzerWebClient() {
        return WebClient.builder()
                .baseUrl(analyzerServiceUrl)
                .build();
    }

    @Bean(name = "generatorWebClient")
    public WebClient generatorWebClient() {
        return WebClient.builder()
                .baseUrl(generatorServiceUrl)
                .build();
    }
}
