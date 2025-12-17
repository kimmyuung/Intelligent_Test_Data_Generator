package com.itdg.generator.controller;

import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.batch.BatchJobLauncherService;
import com.itdg.generator.batch.BatchJobLauncherService.JobExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Batch Job 관리 API
 * 
 * - Job 시작
 * - 진행 상태 조회 (SSE)
 * - 실행중인 Job 목록
 */
@Slf4j
@RestController
@RequestMapping("/api/generator/batch")
@RequiredArgsConstructor
public class BatchJobController {

    private final BatchJobLauncherService batchService;

    /**
     * 대용량 데이터 생성 Job 시작
     */
    @PostMapping("/start")
    public ResponseEntity<JobLaunchResponse> startGeneration(
            @RequestBody BatchGenerateRequest request) {

        log.info("Received batch job request - table: {}, rows: {}",
                request.getTableName(), request.getRowCount());

        try {
            long seed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();

            JobExecution execution = batchService.launchDataGenerationJob(
                    request.getTableName(),
                    request.getSchema(),
                    request.getRowCount(),
                    seed);

            return ResponseEntity.accepted().body(
                    JobLaunchResponse.builder()
                            .executionId(execution.getId())
                            .status(execution.getStatus().name())
                            .message("Job launched successfully")
                            .build());
        } catch (Exception e) {
            log.error("Failed to launch batch job", e);
            return ResponseEntity.internalServerError().body(
                    JobLaunchResponse.builder()
                            .status("FAILED")
                            .message("Failed to launch job: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Job 상태 조회
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<JobExecutionStatus> getJobStatus(@PathVariable Long executionId) {
        JobExecutionStatus status = batchService.getJobStatus(executionId);
        return ResponseEntity.ok(status);
    }

    /**
     * SSE로 Job 진행 상태 스트리밍
     */
    @GetMapping(value = "/status/{executionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<JobExecutionStatus> streamJobStatus(@PathVariable Long executionId) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> batchService.getJobStatus(executionId))
                .takeUntil(JobExecutionStatus::isComplete)
                .doOnComplete(() -> log.info("SSE stream completed for job: {}", executionId));
    }

    /**
     * 실행중인 Job 목록 조회
     */
    @GetMapping("/running")
    public ResponseEntity<List<JobExecutionStatus>> getRunningJobs() {
        return ResponseEntity.ok(batchService.getRunningJobs());
    }

    /**
     * Batch 생성 요청 DTO
     */
    @Data
    public static class BatchGenerateRequest {
        private String tableName;
        private TableMetadata schema;
        private int rowCount;
        private Long seed;
    }

    /**
     * Job 실행 응답 DTO
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class JobLaunchResponse {
        private Long executionId;
        private String status;
        private String message;
    }
}
