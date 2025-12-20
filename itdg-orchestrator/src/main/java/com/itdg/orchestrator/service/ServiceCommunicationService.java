package com.itdg.orchestrator.service;

import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.request.DbConnectionRequest;
import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.response.ApiResponse;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.common.dto.response.HealthCheckResponse;
import com.itdg.orchestrator.dto.MlServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ServiceCommunicationService {

    private final WebClient analyzerWebClient;
    private final WebClient generatorWebClient;
    private final WebClient mlServerWebClient;

    public ServiceCommunicationService(
            @Qualifier("analyzerWebClient") WebClient analyzerWebClient,
            @Qualifier("generatorWebClient") WebClient generatorWebClient,
            @Qualifier("mlServerWebClient") WebClient mlServerWebClient) {
        this.analyzerWebClient = analyzerWebClient;
        this.generatorWebClient = generatorWebClient;
        this.mlServerWebClient = mlServerWebClient;
    }

    /**
     * Analyzer 서비스의 헬스체크 호출
     */
    public Mono<ApiResponse<HealthCheckResponse>> checkAnalyzerHealth() {
        log.info("Calling Analyzer health check");
        return analyzerWebClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<HealthCheckResponse>>() {
                })
                .doOnSuccess(response -> log.info("Analyzer health check successful: {}", response))
                .doOnError(error -> log.error("Analyzer health check failed", error));
    }

    /**
     * Generator 서비스의 헬스체크 호출
     */
    public Mono<ApiResponse<HealthCheckResponse>> checkGeneratorHealth() {
        log.info("Calling Generator health check");
        return generatorWebClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<HealthCheckResponse>>() {
                })
                .doOnSuccess(response -> log.info("Generator health check successful: {}", response))
                .doOnError(error -> log.error("Generator health check failed", error));
    }

    /**
     * ML Server 헬스체크 호출
     */
    public Mono<MlServerDto.HealthResponse> checkMlServerHealth() {
        log.info("Calling ML Server health check");
        return mlServerWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(MlServerDto.HealthResponse.class)
                .doOnSuccess(response -> log.info("ML Server health check successful: {}", response))
                .doOnError(error -> log.error("ML Server health check failed", error));
    }

    /**
     * Analyzer 서비스에 DB 스키마 추출 요청
     */
    public Mono<ApiResponse<SchemaMetadata>> extractSchema(DbConnectionRequest request) {
        log.info("Calling Analyzer extract schema");
        return analyzerWebClient.post()
                .uri("/api/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SchemaMetadata>>() {
                })
                .doOnSuccess(response -> log.info("Schema extraction successful"))
                .doOnError(error -> log.error("Schema extraction failed", error));
    }

    /**
     * Generator 서비스에 데이터 생성 요청
     */
    public Mono<GenerateDataResponse> generateData(GenerateDataRequest request) {
        log.info("Calling Generator generate data");
        return generatorWebClient.post()
                .uri("/api/generator/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenerateDataResponse.class)
                .doOnSuccess(response -> log.info("Data generation request successful"))
                .doOnError(error -> log.error("Data generation request failed", error));
    }

    /**
     * 모든 서비스의 헬스체크를 동시에 호출 (ML Server 포함)
     */
    public Mono<Map<String, Object>> checkAllServicesHealth() {
        log.info("Checking all services health");

        Mono<ApiResponse<HealthCheckResponse>> analyzerHealth = checkAnalyzerHealth()
                .onErrorResume(error -> {
                    log.error("Analyzer service is down", error);
                    return Mono.just(ApiResponse.error("Analyzer service is unavailable"));
                });

        Mono<ApiResponse<HealthCheckResponse>> generatorHealth = checkGeneratorHealth()
                .onErrorResume(error -> {
                    log.error("Generator service is down", error);
                    return Mono.just(ApiResponse.error("Generator service is unavailable"));
                });

        Mono<MlServerDto.HealthResponse> mlServerHealth = checkMlServerHealth()
                .onErrorResume(error -> {
                    log.error("ML Server is down", error);
                    return Mono.just(MlServerDto.HealthResponse.builder().status("unavailable").build());
                });

        return Mono.zip(analyzerHealth, generatorHealth, mlServerHealth)
                .map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("analyzer", tuple.getT1());
                    result.put("generator", tuple.getT2());
                    result.put("mlServer", tuple.getT3());
                    return result;
                });
    }

    /**
     * ML Server에서 합성 데이터 생성
     */
    public Mono<MlServerDto.GenerateResponse> generateMlSyntheticData(String modelId, int numRows) {
        log.info("Calling ML Server generate synthetic data - modelId: {}, numRows: {}", modelId, numRows);
        return mlServerWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/generate/{modelId}")
                        .queryParam("num_rows", numRows)
                        .build(modelId))
                .retrieve()
                .bodyToMono(MlServerDto.GenerateResponse.class)
                .doOnSuccess(response -> log.info("ML synthetic data generated: {} rows", response.getRowCount()))
                .doOnError(error -> log.error("ML synthetic data generation failed", error));
    }
}
