package com.itdg.orchestrator.controller;

import com.itdg.orchestrator.dto.MlServerDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * MlProxyController 통합 테스트
 * 
 * WebClient Mock 복잡성 때문에 간단한 DTO 검증만 수행
 */
@DisplayName("MlServerDto 테스트")
class MlServerDtoTest {

        @Test
        @DisplayName("TrainResponse Builder 테스트")
        void trainResponse_builderTest() {
                // Given & When
                MlServerDto.TrainResponse response = MlServerDto.TrainResponse.builder()
                                .success(true)
                                .modelId("test-model-id")
                                .status("trained")
                                .modelType("copula")
                                .columns(List.of("id", "name"))
                                .rowCount(100)
                                .trainingTime(1.5)
                                .build();

                // Then
                assert response.isSuccess();
                assert "test-model-id".equals(response.getModelId());
                assert "trained".equals(response.getStatus());
                assert "copula".equals(response.getModelType());
                assert response.getRowCount() == 100;
        }

        @Test
        @DisplayName("GenerateResponse Builder 테스트")
        void generateResponse_builderTest() {
                // Given & When
                MlServerDto.GenerateResponse response = MlServerDto.GenerateResponse.builder()
                                .success(true)
                                .data(List.of(
                                                Map.of("id", 1, "name", "Test1"),
                                                Map.of("id", 2, "name", "Test2")))
                                .columns(List.of("id", "name"))
                                .rowCount(2)
                                .build();

                // Then
                assert response.isSuccess();
                assert response.getData().size() == 2;
                assert response.getRowCount() == 2;
        }

        @Test
        @DisplayName("ModelInfoResponse Builder 테스트")
        void modelInfoResponse_builderTest() {
                // Given & When
                MlServerDto.ModelInfoResponse response = MlServerDto.ModelInfoResponse.builder()
                                .success(true)
                                .modelId("test-model-id")
                                .exists(true)
                                .size(1024)
                                .createdAt(System.currentTimeMillis())
                                .expiresIn(3600.0)
                                .build();

                // Then
                assert response.isSuccess();
                assert response.isExists();
                assert "test-model-id".equals(response.getModelId());
        }

        @Test
        @DisplayName("HealthResponse Builder 테스트")
        void healthResponse_builderTest() {
                // Given & When
                MlServerDto.HealthResponse response = MlServerDto.HealthResponse.builder()
                                .status("healthy")
                                .build();

                // Then
                assert "healthy".equals(response.getStatus());
        }

        @Test
        @DisplayName("SimpleResponse Builder 테스트")
        void simpleResponse_builderTest() {
                // Given & When
                MlServerDto.SimpleResponse response = MlServerDto.SimpleResponse.builder()
                                .success(true)
                                .message("Operation completed")
                                .build();

                // Then
                assert response.isSuccess();
                assert "Operation completed".equals(response.getMessage());
        }
}
