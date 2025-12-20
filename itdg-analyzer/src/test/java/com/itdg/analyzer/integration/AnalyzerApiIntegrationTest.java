package com.itdg.analyzer.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Analyzer API 통합 테스트 (TestRestTemplate 사용)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Analyzer API 통합 테스트")
class AnalyzerApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("헬스체크 API 성공")
    void healthCheck_ShouldReturnUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    @Order(2)
    @DisplayName("DB 분석 - 유효하지 않은 URL")
    void analyzeDb_WithInvalidUrl_ShouldReturnError() {
        String requestBody = """
                {
                    "url": "invalid-url",
                    "username": "user",
                    "password": "pass"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/analyze", entity, Map.class);

        // 400 또는 500 에러 예상
        assertThat(response.getStatusCode().value())
                .isIn(400, 500);
    }

    @Test
    @Order(3)
    @DisplayName("Git 분석 - 유효하지 않은 저장소")
    void analyzeGit_WithInvalidRepo_ShouldReturnError() {
        String requestBody = """
                {
                    "repoUrl": "https://github.com/invalid/non-existent-repo"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/analyze/git", entity, Map.class);

        assertThat(response.getStatusCode().value())
                .isIn(400, 500);
    }

    @Test
    @Order(4)
    @DisplayName("빈 요청 - 유효성 검증 실패")
    void analyzeDb_WithEmptyRequest_ShouldReturnBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/analyze", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
