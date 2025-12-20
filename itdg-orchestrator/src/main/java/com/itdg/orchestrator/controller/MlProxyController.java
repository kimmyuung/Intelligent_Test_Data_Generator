package com.itdg.orchestrator.controller;

import com.itdg.orchestrator.dto.MlServerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * ML Server API 프록시 컨트롤러
 * 
 * 프론트엔드가 ML Server를 직접 호출하지 않고 Orchestrator를 통해 호출하도록 함
 * - CORS 문제 해결
 * - 보안 강화 (ML Server를 내부망에 배치 가능)
 * - 로깅 및 모니터링 중앙화
 */
@Slf4j
@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MlProxyController {

        private final WebClient mlServerWebClient;

        public MlProxyController(@Qualifier("mlServerWebClient") WebClient mlServerWebClient) {
                this.mlServerWebClient = mlServerWebClient;
        }

        /**
         * ML Server 헬스체크 프록시
         */
        @GetMapping("/health")
        public Mono<MlServerDto.HealthResponse> healthCheck() {
                log.info("ML Proxy: Health check");
                return mlServerWebClient.get()
                                .uri("/health")
                                .retrieve()
                                .bodyToMono(MlServerDto.HealthResponse.class)
                                .doOnSuccess(r -> log.info("ML Server health: {}", r.getStatus()))
                                .doOnError(e -> log.error("ML Server health check failed", e));
        }

        /**
         * 파일 분석 프록시
         * POST /api/ml/analyze
         */
        @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Mono<Map<String, Object>> analyzeFile(@RequestPart("file") FilePart file) {
                log.info("ML Proxy: Analyze file - {}", file.filename());

                return DataBufferUtils.join(file.content())
                                .flatMap(dataBuffer -> {
                                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(bytes);
                                        DataBufferUtils.release(dataBuffer);

                                        MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                        builder.part("file", bytes)
                                                        .filename(file.filename())
                                                        .contentType(MediaType.APPLICATION_OCTET_STREAM);

                                        return mlServerWebClient.post()
                                                        .uri("/api/v1/analyze")
                                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                                        .retrieve()
                                                        .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                                        });
                                })
                                .doOnSuccess(r -> log.info("ML Proxy: Analyze success - fileId: {}", r.get("fileId")))
                                .doOnError(e -> log.error("ML Proxy: Analyze failed", e));
        }

        /**
         * 모델 학습 프록시
         * POST /api/ml/train?file_id=xxx&model_type=copula
         */
        @PostMapping("/train")
        public Mono<MlServerDto.TrainResponse> trainModel(
                        @RequestParam("file_id") String fileId,
                        @RequestParam(value = "model_type", defaultValue = "copula") String modelType) {

                log.info("ML Proxy: Train model - fileId: {}, modelType: {}", fileId, modelType);

                return mlServerWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v1/train")
                                                .queryParam("file_id", fileId)
                                                .queryParam("model_type", modelType)
                                                .build())
                                .retrieve()
                                .bodyToMono(MlServerDto.TrainResponse.class)
                                .doOnSuccess(r -> log.info("ML Proxy: Train success - modelId: {}, time: {}s",
                                                r.getModelId(), r.getTrainingTime()))
                                .doOnError(e -> log.error("ML Proxy: Train failed", e));
        }

        /**
         * 합성 데이터 생성 프록시
         * POST /api/ml/generate/{modelId}?num_rows=100
         */
        @PostMapping("/generate/{modelId}")
        public Mono<MlServerDto.GenerateResponse> generateData(
                        @PathVariable String modelId,
                        @RequestParam(value = "num_rows", defaultValue = "100") int numRows) {

                log.info("ML Proxy: Generate data - modelId: {}, numRows: {}", modelId, numRows);

                return mlServerWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v1/generate/{modelId}")
                                                .queryParam("num_rows", numRows)
                                                .build(modelId))
                                .retrieve()
                                .bodyToMono(MlServerDto.GenerateResponse.class)
                                .doOnSuccess(r -> log.info("ML Proxy: Generate success - {} rows", r.getRowCount()))
                                .doOnError(e -> log.error("ML Proxy: Generate failed", e));
        }

        /**
         * 모델 정보 조회 프록시
         * GET /api/ml/model/{modelId}
         */
        @GetMapping("/model/{modelId}")
        public Mono<MlServerDto.ModelInfoResponse> getModelInfo(@PathVariable String modelId) {
                log.info("ML Proxy: Get model info - modelId: {}", modelId);

                return mlServerWebClient.get()
                                .uri("/api/v1/model/{modelId}", modelId)
                                .retrieve()
                                .bodyToMono(MlServerDto.ModelInfoResponse.class)
                                .doOnSuccess(r -> log.info("ML Proxy: Model info - exists: {}", r.isExists()))
                                .doOnError(e -> log.error("ML Proxy: Get model info failed", e));
        }

        /**
         * 모델 삭제 프록시
         * DELETE /api/ml/model/{modelId}
         */
        @DeleteMapping("/model/{modelId}")
        public Mono<MlServerDto.SimpleResponse> deleteModel(@PathVariable String modelId) {
                log.info("ML Proxy: Delete model - modelId: {}", modelId);

                return mlServerWebClient.delete()
                                .uri("/api/v1/model/{modelId}", modelId)
                                .retrieve()
                                .bodyToMono(MlServerDto.SimpleResponse.class)
                                .doOnSuccess(r -> log.info("ML Proxy: Model deleted - {}", r.getMessage()))
                                .doOnError(e -> log.error("ML Proxy: Delete model failed", e));
        }

        // ========================================
        // 다중 테이블 학습 API (Multi-Table Learning)
        // ========================================

        /**
         * 다중 테이블 학습 프록시
         * POST /api/ml/multi-table/train
         */
        @PostMapping("/multi-table/train")
        public Mono<Map<String, Object>> trainMultiTable(@RequestBody Map<String, Object> request) {
                log.info("ML Proxy: Multi-table train - tables: {}", request.get("tables"));

                return mlServerWebClient.post()
                                .uri("/api/v1/multi-table/train")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .doOnSuccess(r -> log.info("ML Proxy: Multi-table train success - modelId: {}",
                                                r.get("modelId")))
                                .doOnError(e -> log.error("ML Proxy: Multi-table train failed", e));
        }

        /**
         * 다중 테이블 데이터 생성 프록시
         * POST /api/ml/multi-table/generate/{modelId}?scale=1.0
         */
        @PostMapping("/multi-table/generate/{modelId}")
        public Mono<Map<String, Object>> generateMultiTable(
                        @PathVariable String modelId,
                        @RequestParam(value = "scale", defaultValue = "1.0") double scale) {

                log.info("ML Proxy: Multi-table generate - modelId: {}, scale: {}", modelId, scale);

                return mlServerWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v1/multi-table/generate/{modelId}")
                                                .queryParam("scale", scale)
                                                .build(modelId))
                                .retrieve()
                                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .doOnSuccess(r -> log.info("ML Proxy: Multi-table generate success"))
                                .doOnError(e -> log.error("ML Proxy: Multi-table generate failed", e));
        }
}
