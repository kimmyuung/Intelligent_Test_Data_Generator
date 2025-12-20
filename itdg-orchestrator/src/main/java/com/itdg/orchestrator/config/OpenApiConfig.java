package com.itdg.orchestrator.config;

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
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * API Docs: http://localhost:8080/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orchestratorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ITDG Orchestrator API")
                        .description("Intelligent Test Data Generator - 오케스트레이터 서비스 API\n\n" +
                                "이 서비스는 데이터 생성 워크플로우를 조율하고 다른 마이크로서비스들과 통신합니다.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ITDG Team")
                                .email("itdg@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("http://orchestrator:8080").description("Docker Environment")));
    }
}
