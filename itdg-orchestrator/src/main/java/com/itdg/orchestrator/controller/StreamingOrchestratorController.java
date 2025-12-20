package com.itdg.orchestrator.controller;

import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.orchestrator.dto.MlServerDto;
import com.itdg.orchestrator.service.ServiceCommunicationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스트리밍 기반 데이터 생성 API (Orchestrator)
 * 
 * Generator 서비스로부터 스트리밍 데이터를 받아 SSE로 Frontend에 전달
 * - 실시간 진행률 표시
 * - 파일 다운로드 프록시
 */
@Slf4j
@RestController
@RequestMapping("/api/orchestrator/stream")
@CrossOrigin(origins = "*")
public class StreamingOrchestratorController {

        private final WebClient generatorWebClient;
        private final ServiceCommunicationService communicationService;

        public StreamingOrchestratorController(
                        @Qualifier("generatorWebClient") WebClient generatorWebClient,
                        ServiceCommunicationService communicationService) {
                this.generatorWebClient = generatorWebClient;
                this.communicationService = communicationService;
        }

        /**
         * SSE 실시간 스트리밍 (프론트엔드용)
         * Generator → Orchestrator → Frontend 파이프라인
         */
        @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<DataChunk>> streamGenerate(
                        @RequestBody StreamGenerateRequest request) {

                log.info("Starting SSE streaming - table: {}, rows: {}",
                                request.getTableName(), request.getRowCount());

                AtomicInteger processed = new AtomicInteger(0);
                int totalRows = request.getRowCount();

                return generatorWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/generator/stream/ndjson")
                                                .queryParam("rowCount", request.getRowCount())
                                                .queryParam("seed", request.getSeed() != null ? request.getSeed() : 0)
                                                .build())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request.getSchema())
                                .retrieve()
                                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .buffer(100) // 100개씩 묶어서 전송 (네트워크 효율)
                                .map(rows -> {
                                        int current = processed.addAndGet(rows.size());
                                        return ServerSentEvent.<DataChunk>builder()
                                                        .id(String.valueOf(current))
                                                        .event("data")
                                                        .data(DataChunk.builder()
                                                                        .rows(rows)
                                                                        .progress(current)
                                                                        .total(totalRows)
                                                                        .percentComplete((int) ((current * 100L)
                                                                                        / totalRows))
                                                                        .build())
                                                        .build();
                                })
                                .concatWith(Flux.just(
                                                ServerSentEvent.<DataChunk>builder()
                                                                .event("complete")
                                                                .data(DataChunk.builder()
                                                                                .progress(totalRows)
                                                                                .total(totalRows)
                                                                                .percentComplete(100)
                                                                                .build())
                                                                .build()))
                                .doOnComplete(() -> log.info("SSE streaming completed - {} rows", processed.get()))
                                .doOnError(error -> log.error("SSE streaming error", error));
        }

        /**
         * ML 합성 데이터 생성 엔드포인트
         * 학습된 모델로 합성 데이터를 생성하여 SSE로 반환
         */
        @PostMapping(value = "/generate-ml", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<DataChunk>> streamGenerateWithMl(
                        @RequestBody StreamGenerateRequest request) {

                if (request.getMlModelId() == null || request.getMlModelId().isEmpty()) {
                        return Flux.error(new IllegalArgumentException("mlModelId is required for ML generation"));
                }

                log.info("Starting ML-based SSE streaming - table: {}, rows: {}, modelId: {}",
                                request.getTableName(), request.getRowCount(), request.getMlModelId());

                int totalRows = request.getRowCount();

                // ML Server에서 합성 데이터 가져오기
                return communicationService.generateMlSyntheticData(request.getMlModelId(), totalRows)
                                .flatMapMany(mlResponse -> {
                                        if (!mlResponse.isSuccess() || mlResponse.getData() == null) {
                                                return Flux.error(new RuntimeException("ML data generation failed"));
                                        }

                                        List<Map<String, Object>> allData = mlResponse.getData();
                                        List<List<Map<String, Object>>> chunks = new ArrayList<>();

                                        // 100개씩 청크로 분할
                                        for (int i = 0; i < allData.size(); i += 100) {
                                                chunks.add(allData.subList(i, Math.min(i + 100, allData.size())));
                                        }

                                        AtomicInteger processed = new AtomicInteger(0);

                                        return Flux.fromIterable(chunks)
                                                        .map(rows -> {
                                                                int current = processed.addAndGet(rows.size());
                                                                return ServerSentEvent.<DataChunk>builder()
                                                                                .id(String.valueOf(current))
                                                                                .event("data")
                                                                                .data(DataChunk.builder()
                                                                                                .rows(rows)
                                                                                                .progress(current)
                                                                                                .total(totalRows)
                                                                                                .percentComplete(
                                                                                                                (int) ((current * 100L)
                                                                                                                                / totalRows))
                                                                                                .mlGenerated(true)
                                                                                                .build())
                                                                                .build();
                                                        });
                                })
                                .concatWith(Flux.just(
                                                ServerSentEvent.<DataChunk>builder()
                                                                .event("complete")
                                                                .data(DataChunk.builder()
                                                                                .progress(totalRows)
                                                                                .total(totalRows)
                                                                                .percentComplete(100)
                                                                                .mlGenerated(true)
                                                                                .build())
                                                                .build()))
                                .doOnComplete(() -> log.info("ML SSE streaming completed - {} rows", totalRows))
                                .doOnError(error -> log.error("ML SSE streaming error", error));
        }

        /**
         * CSV 파일 다운로드 프록시 (스트리밍 유지)
         */
        @PostMapping("/download/csv")
        public Mono<ResponseEntity<Flux<DataBuffer>>> downloadCsv(
                        @RequestBody StreamGenerateRequest request) {

                log.info("Starting CSV download proxy - table: {}, rows: {}",
                                request.getTableName(), request.getRowCount());

                return Mono.just(
                                ResponseEntity.ok()
                                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                                "attachment; filename=\"" + request.getTableName()
                                                                                + ".csv\"")
                                                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                                                .body(
                                                                generatorWebClient.post()
                                                                                .uri(uriBuilder -> uriBuilder
                                                                                                .path("/api/generator/stream/csv")
                                                                                                .queryParam("rowCount",
                                                                                                                request.getRowCount())
                                                                                                .queryParam("seed",
                                                                                                                request.getSeed() != null
                                                                                                                                ? request.getSeed()
                                                                                                                                : 0)
                                                                                                .build())
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .bodyValue(request.getSchema())
                                                                                .retrieve()
                                                                                .bodyToFlux(DataBuffer.class)));
        }

        /**
         * JSON 파일 다운로드 프록시
         */
        @PostMapping("/download/json")
        public Mono<ResponseEntity<Flux<DataBuffer>>> downloadJson(
                        @RequestBody StreamGenerateRequest request) {

                log.info("Starting JSON download proxy - table: {}, rows: {}",
                                request.getTableName(), request.getRowCount());

                return Mono.just(
                                ResponseEntity.ok()
                                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                                "attachment; filename=\"" + request.getTableName()
                                                                                + ".json\"")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(
                                                                generatorWebClient.post()
                                                                                .uri(uriBuilder -> uriBuilder
                                                                                                .path("/api/generator/stream/json")
                                                                                                .queryParam("rowCount",
                                                                                                                request.getRowCount())
                                                                                                .queryParam("seed",
                                                                                                                request.getSeed() != null
                                                                                                                                ? request.getSeed()
                                                                                                                                : 0)
                                                                                                .build())
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .bodyValue(request.getSchema())
                                                                                .retrieve()
                                                                                .bodyToFlux(DataBuffer.class)));
        }

        /**
         * XLSX 파일 다운로드 프록시
         */
        @PostMapping("/download/xlsx")
        public Mono<ResponseEntity<Flux<DataBuffer>>> downloadXlsx(
                        @RequestBody StreamGenerateRequest request) {

                log.info("Starting XLSX download proxy - table: {}, rows: {}",
                                request.getTableName(), request.getRowCount());

                return Mono.just(
                                ResponseEntity.ok()
                                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                                "attachment; filename=\"" + request.getTableName()
                                                                                + ".xlsx\"")
                                                .contentType(MediaType.parseMediaType(
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                                .body(
                                                                generatorWebClient.post()
                                                                                .uri(uriBuilder -> uriBuilder
                                                                                                .path("/api/generator/stream/xlsx")
                                                                                                .queryParam("rowCount",
                                                                                                                request.getRowCount())
                                                                                                .queryParam("seed",
                                                                                                                request.getSeed() != null
                                                                                                                                ? request.getSeed()
                                                                                                                                : 0)
                                                                                                .build())
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .bodyValue(request.getSchema())
                                                                                .retrieve()
                                                                                .bodyToFlux(DataBuffer.class)));
        }

        /**
         * 스트리밍 생성 요청 DTO
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class StreamGenerateRequest {
                private String tableName;
                private TableMetadata schema;
                private int rowCount;
                private Long seed;
                private String mlModelId; // ML 학습 모델 ID (있으면 ML 합성 데이터 사용)
        }

        /**
         * 데이터 청크 DTO
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DataChunk {
                private List<Map<String, Object>> rows;
                private int progress;
                private int total;
                private int percentComplete;
                private boolean mlGenerated; // ML 합성 데이터 여부
        }
}
