package com.itdg.orchestrator.service;

import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.common.dto.request.DbConnectionRequest;
import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.request.OrchestrationRequest;
import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.GenerateDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrchestrationService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrchestrationService 테스트")
class OrchestrationServiceTest {

        @Mock
        private ServiceCommunicationService communicationService;

        private OrchestrationService orchestrationService;

        @BeforeEach
        void setUp() {
                orchestrationService = new OrchestrationService(communicationService);
        }

        // =========================================
        // Helper Methods
        // =========================================

        private DbConnectionRequest createDbConnection() {
                return DbConnectionRequest.builder()
                                .url("jdbc:postgresql://localhost:5432/testdb")
                                .username("testuser")
                                .password("testpass")
                                .build();
        }

        private OrchestrationRequest createOrchestrationRequest() {
                return OrchestrationRequest.builder()
                                .dbConnection(createDbConnection())
                                .rowCount(100)
                                .seed(12345L)
                                .build();
        }

        private SchemaMetadata createMockSchema() {
                TableMetadata table = TableMetadata.builder()
                                .tableName("users")
                                .columns(List.of())
                                .build();
                return SchemaMetadata.builder()
                                .databaseName("testdb")
                                .tables(List.of(table))
                                .build();
        }

        private ApiResponse<SchemaMetadata> createSuccessSchemaResponse(SchemaMetadata schema) {
                return ApiResponse.<SchemaMetadata>builder()
                                .success(true)
                                .data(schema)
                                .message("Schema extracted successfully")
                                .build();
        }

        private ApiResponse<SchemaMetadata> createFailureSchemaResponse() {
                return ApiResponse.<SchemaMetadata>builder()
                                .success(false)
                                .data(null)
                                .message("Failed to extract schema")
                                .build();
        }

        private GenerateDataResponse createSuccessGenerateResponse() {
                return GenerateDataResponse.builder()
                                .success(true)
                                .generatedData(Map.of("users", List.of(Map.of("id", 1))))
                                .statistics(Map.of("users", 1))
                                .generatedAt(LocalDateTime.now())
                                .seed(12345L)
                                .message("Data generated successfully")
                                .build();
        }

        // =========================================
        // processDataGeneration() 테스트
        // =========================================

        @Nested
        @DisplayName("processDataGeneration() 메서드")
        class ProcessDataGenerationTests {

                @Test
                @DisplayName("정상적인 요청으로 데이터 생성 성공")
                void processDataGeneration_withValidRequest_returnsSuccess() {
                        // Given
                        OrchestrationRequest request = createOrchestrationRequest();
                        SchemaMetadata schema = createMockSchema();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.just(createSuccessSchemaResponse(schema)));
                        when(communicationService.generateData(any(GenerateDataRequest.class)))
                                        .thenReturn(Mono.just(createSuccessGenerateResponse()));

                        // When & Then
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .assertNext(response -> {
                                                assertThat(response).isNotNull();
                                                assertThat(response.isSuccess()).isTrue();
                                                assertThat(response.getGeneratedData()).containsKey("users");
                                        })
                                        .verifyComplete();

                        // Verify interactions
                        verify(communicationService).extractSchema(any(DbConnectionRequest.class));
                        verify(communicationService).generateData(any(GenerateDataRequest.class));
                }

                @Test
                @DisplayName("스키마 추출 실패 시 에러 반환")
                void processDataGeneration_whenSchemaExtractionFails_returnsError() {
                        // Given
                        OrchestrationRequest request = createOrchestrationRequest();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.just(createFailureSchemaResponse()));

                        // When & Then
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                        error.getMessage().contains("Failed to extract schema"))
                                        .verify();

                        // Generator는 호출되지 않아야 함
                        verify(communicationService).extractSchema(any(DbConnectionRequest.class));
                        verify(communicationService, never()).generateData(any(GenerateDataRequest.class));
                }

                @Test
                @DisplayName("스키마 추출 응답의 data가 null이면 에러 반환")
                void processDataGeneration_whenSchemaDataIsNull_returnsError() {
                        // Given
                        OrchestrationRequest request = createOrchestrationRequest();
                        ApiResponse<SchemaMetadata> responseWithNullData = ApiResponse.<SchemaMetadata>builder()
                                        .success(true)
                                        .data(null)
                                        .message("Success but no data")
                                        .build();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.just(responseWithNullData));

                        // When & Then
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .expectError(RuntimeException.class)
                                        .verify();
                }

                @Test
                @DisplayName("Analyzer 서비스 에러 시 에러 전파")
                void processDataGeneration_whenAnalyzerServiceFails_propagatesError() {
                        // Given
                        OrchestrationRequest request = createOrchestrationRequest();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.error(new RuntimeException("Analyzer service unavailable")));

                        // When & Then
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                        error.getMessage().contains("Analyzer service unavailable"))
                                        .verify();
                }

                @Test
                @DisplayName("Generator 서비스 에러 시 에러 전파")
                void processDataGeneration_whenGeneratorServiceFails_propagatesError() {
                        // Given
                        OrchestrationRequest request = createOrchestrationRequest();
                        SchemaMetadata schema = createMockSchema();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.just(createSuccessSchemaResponse(schema)));
                        when(communicationService.generateData(any(GenerateDataRequest.class)))
                                        .thenReturn(Mono.error(new RuntimeException("Generator service unavailable")));

                        // When & Then
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .expectErrorMatches(error -> error instanceof RuntimeException &&
                                                        error.getMessage().contains("Generator service unavailable"))
                                        .verify();
                }

                @Test
                @DisplayName("요청의 rowCount와 seed가 Generator에 전달되는지 확인")
                void processDataGeneration_passesCorrectParametersToGenerator() {
                        // Given
                        OrchestrationRequest request = OrchestrationRequest.builder()
                                        .dbConnection(createDbConnection())
                                        .rowCount(500)
                                        .seed(99999L)
                                        .build();
                        SchemaMetadata schema = createMockSchema();

                        when(communicationService.extractSchema(any(DbConnectionRequest.class)))
                                        .thenReturn(Mono.just(createSuccessSchemaResponse(schema)));
                        when(communicationService.generateData(any(GenerateDataRequest.class)))
                                        .thenReturn(Mono.just(createSuccessGenerateResponse()));

                        // When
                        StepVerifier.create(orchestrationService.processDataGeneration(request))
                                        .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                                        .verifyComplete();

                        // Then - verify the GenerateDataRequest parameters
                        verify(communicationService).generateData(
                                        argThat(req -> req.getRowCount() == 500 && req.getSeed() == 99999L));
                }
        }
}
