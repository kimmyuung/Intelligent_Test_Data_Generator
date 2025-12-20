package com.itdg.orchestrator.integration;

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
 * Orchestrator API 통합 테스트 (TestRestTemplate 사용)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Orchestrator API 통합 테스트")
class OrchestratorApiIntegrationTest {

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
        @DisplayName("서비스 상태 확인 API")
        void testAllServices_ShouldReturnStatus() {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                                getBaseUrl() + "/api/test/all", Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsKey("orchestrator");
        }

        @Test
        @Order(3)
        @DisplayName("잘못된 DB 연결 요청 - 유효성 검증 실패")
        void processOrchestration_WithInvalidRequest_ShouldReturnBadRequest() {
                String requestBody = """
                                {
                                    "dbConnection": {
                                        "url": "",
                                        "username": "",
                                        "password": ""
                                    }
                                }
                                """;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                                getBaseUrl() + "/api/orchestrator/process", entity, Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @Order(4)
        @DisplayName("ML 헬스체크 프록시")
        void mlHealthProxy_ShouldForwardRequest() {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                                getBaseUrl() + "/api/ml/health", Map.class);

                // ML 서버가 실행 중이면 200, 아니면 503/500
                assertThat(response.getStatusCode().value())
                                .isIn(200, 500, 503);
        }

        @Test
        @Order(5)
        @DisplayName("다운로드 엔드포인트 - 빈 요청")
        void downloadCsv_WithEmptyRequest_ShouldReturnBadRequest() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>("{}", headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                                getBaseUrl() + "/api/orchestrator/stream/download/csv", entity, Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @Order(6)
        @DisplayName("존재하지 않는 모델 조회")
        void getModelInfo_WithNonExistentModel_ShouldReturnError() {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                                getBaseUrl() + "/api/ml/model/non-existent-model-id", Map.class);

                assertThat(response.getStatusCode().value())
                                .isIn(404, 500, 503);
        }
}
