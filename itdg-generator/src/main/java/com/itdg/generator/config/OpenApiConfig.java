package com.itdg.generator.config;

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
 * Swagger UI: http://localhost:8082/swagger-ui.html
 * API Docs: http://localhost:8082/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI generatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ITDG Generator API")
                        .description("Intelligent Test Data Generator - 생성기 서비스 API\n\n" +
                                "이 서비스는 패턴 기반 테스트 데이터 생성을 수행합니다.\n" +
                                "지원 생성기: 이름, 이메일, 전화번호, 주소, 날짜, 숫자, 불리언, URL, UUID")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ITDG Team")
                                .email("itdg@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Development"),
                        new Server().url("http://generator:8082").description("Docker Environment")));
    }
}
