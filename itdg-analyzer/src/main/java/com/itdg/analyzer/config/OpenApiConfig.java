package com.itdg.analyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 설정 클래스
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * API Docs: http://localhost:8081/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyzerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ITDG Analyzer API")
                        .description("Intelligent Test Data Generator - 분석기 서비스 API\n\n" +
                                "이 서비스는 데이터베이스 스키마 분석, Git 리포지토리 분석(JPA Entity, SQL DDL 파싱)을 수행합니다.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ITDG Team")
                                .email("itdg@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development"),
                        new Server().url("http://analyzer:8081").description("Docker Environment")));
    }
}
