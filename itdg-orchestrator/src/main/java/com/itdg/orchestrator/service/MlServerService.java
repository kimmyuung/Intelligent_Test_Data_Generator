package com.itdg.orchestrator.service;

import com.itdg.orchestrator.dto.MlServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * ML Server (Python FastAPI) 통신 서비스
 * - 모델 학습 (train)
 * - 합성 데이터 생성 (generate)
 */
@Slf4j
@Service
public class MlServerService {

    private final WebClient mlServerWebClient;

    public MlServerService(@Qualifier("mlServerWebClient") WebClient mlServerWebClient) {
        this.mlServerWebClient = mlServerWebClient;
    }

    /**
     * ML Server 헬스체크
     */
    public Mono<MlServerDto.HealthResponse> checkHealth() {
        log.info("Calling ML Server health check");
        return mlServerWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(MlServerDto.HealthResponse.class)
                .doOnSuccess(response -> log.info("ML Server health check successful: {}", response))
                .doOnError(error -> log.error("ML Server health check failed", error));
    }

    /**
     * 모델 학습 요청
     * 
     * @param fileId    분석된 파일 ID (ML Server에서 발급)
     * @param modelType 모델 타입 ("copula" 또는 "ctgan")
     */
    public Mono<MlServerDto.TrainResponse> trainModel(String fileId, String modelType) {
        log.info("Calling ML Server train model - fileId: {}, modelType: {}", fileId, modelType);
        return mlServerWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/train")
                        .queryParam("file_id", fileId)
                        .queryParam("model_type", modelType)
                        .build())
                .retrieve()
                .bodyToMono(MlServerDto.TrainResponse.class)
                .doOnSuccess(response -> log.info("Model training successful: modelId={}, time={}s",
                        response.getModelId(), response.getTrainingTime()))
                .doOnError(error -> log.error("Model training failed", error));
    }

    /**
     * 합성 데이터 생성
     * 
     * @param modelId 학습된 모델 ID
     * @param numRows 생성할 행 수
     */
    public Mono<MlServerDto.GenerateResponse> generateSyntheticData(String modelId, int numRows) {
        log.info("Calling ML Server generate data - modelId: {}, numRows: {}", modelId, numRows);
        return mlServerWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/generate/{modelId}")
                        .queryParam("num_rows", numRows)
                        .build(modelId))
                .retrieve()
                .bodyToMono(MlServerDto.GenerateResponse.class)
                .doOnSuccess(
                        response -> log.info("Synthetic data generation successful: {} rows", response.getRowCount()))
                .doOnError(error -> log.error("Synthetic data generation failed", error));
    }

    /**
     * 모델 정보 조회
     */
    public Mono<MlServerDto.ModelInfoResponse> getModelInfo(String modelId) {
        log.info("Calling ML Server get model info - modelId: {}", modelId);
        return mlServerWebClient.get()
                .uri("/api/v1/model/{modelId}", modelId)
                .retrieve()
                .bodyToMono(MlServerDto.ModelInfoResponse.class)
                .doOnSuccess(response -> log.info("Model info retrieved: {}", response))
                .doOnError(error -> log.error("Get model info failed", error));
    }

    /**
     * 모델 삭제
     */
    public Mono<MlServerDto.SimpleResponse> deleteModel(String modelId) {
        log.info("Calling ML Server delete model - modelId: {}", modelId);
        return mlServerWebClient.delete()
                .uri("/api/v1/model/{modelId}", modelId)
                .retrieve()
                .bodyToMono(MlServerDto.SimpleResponse.class)
                .doOnSuccess(response -> log.info("Model deleted: {}", response))
                .doOnError(error -> log.error("Delete model failed", error));
    }

    /**
     * 합성 데이터를 Generator에서 사용할 수 있는 형식으로 변환
     * ML 데이터를 컬럼별 값 리스트로 변환
     */
    public Map<String, List<Object>> convertToColumnValues(MlServerDto.GenerateResponse response) {
        if (response.getData() == null || response.getData().isEmpty()) {
            return Map.of();
        }

        // 첫 번째 row에서 컬럼 목록 추출
        List<String> columns = response.getColumns();

        // 각 컬럼별 값 리스트 생성
        return columns.stream()
                .collect(java.util.stream.Collectors.toMap(
                        col -> col,
                        col -> response.getData().stream()
                                .map(row -> row.get(col))
                                .collect(java.util.stream.Collectors.toList())));
    }
}
