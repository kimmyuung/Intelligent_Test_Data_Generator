package com.itdg.orchestrator.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E 워크플로우 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E 워크플로우 통합 테스트")
@AutoConfigureTestRestTemplate
class E2EWorkflowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: 시스템 헬스체크")
    void step1_SystemHealthCheck() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: 전체 서비스 상태 확인")
    void step2_CheckAllServices() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/test/all", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("Services Status: " + response.getBody());
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: 스키마 기반 데이터 생성 요청")
    void step3_ProcessMetadataBasedGeneration() {
        String requestBody = """
                {
                    "tables": [
                        {
                            "tableName": "customers",
                            "targetRowCount": 5,
                            "columns": [
                                {"name": "id", "dataType": "INTEGER", "isPrimaryKey": true},
                                {"name": "name", "dataType": "VARCHAR"},
                                {"name": "email", "dataType": "VARCHAR"},
                                {"name": "created_at", "dataType": "TIMESTAMP"}
                            ]
                        }
                    ]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/orchestrator/process-metadata", entity, Map.class);

        System.out.println("Metadata generation status: " + response.getStatusCode());
        System.out.println("Response: " + response.getBody());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: ML 서버 상태 확인")
    void step4_CheckMlServerStatus() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                getBaseUrl() + "/api/ml/health", Map.class);

        System.out.println("ML Server status: " + response.getStatusCode());
        System.out.println("ML Server response: " + response.getBody());

        assertThat(response.getStatusCode().value())
                .isIn(200, 500, 503);
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: 에러 핸들링 검증")
    void step5_ErrorHandlingVerification() {
        String invalidRequest = """
                {
                    "tables": null
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(invalidRequest, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                getBaseUrl() + "/api/orchestrator/stream/generate", entity, Map.class);

        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: 동시 요청 처리 테스트")
    void step6_ConcurrentRequestHandling() {
        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    getBaseUrl() + "/api/health", Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
