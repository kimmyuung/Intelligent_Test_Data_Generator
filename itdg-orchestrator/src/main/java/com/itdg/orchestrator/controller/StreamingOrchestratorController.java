package com.itdg.orchestrator.controller;

import com.itdg.common.dto.metadata.TableMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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

import java.time.Duration;
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
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StreamingOrchestratorController {

        @Qualifier("generatorWebClient")
        private final WebClient generatorWebClient;

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
        }
}
