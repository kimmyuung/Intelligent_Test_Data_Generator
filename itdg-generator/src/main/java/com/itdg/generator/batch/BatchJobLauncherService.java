package com.itdg.generator.batch;

import com.itdg.common.dto.metadata.TableMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Batch Job 실행 서비스
 * 
 * 비동기 Job 실행 및 상태 조회 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobLauncherService {

    private final JobLauncher jobLauncher;
    private final Job dataGenerationJob;
    private final JobExplorer jobExplorer;
    private final ObjectMapper objectMapper;

    /**
     * 비동기로 대용량 데이터 생성 Job 실행
     */
    @Async
    public CompletableFuture<JobExecution> launchDataGenerationJobAsync(
            String tableName,
            TableMetadata schema,
            int rowCount,
            long seed) {

        try {
            JobExecution execution = launchDataGenerationJob(tableName, schema, rowCount, seed);
            return CompletableFuture.completedFuture(execution);
        } catch (Exception e) {
            log.error("Failed to launch batch job", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 동기적으로 데이터 생성 Job 실행
     */
    public JobExecution launchDataGenerationJob(
            String tableName,
            TableMetadata schema,
            int rowCount,
            long seed) throws Exception {

        String schemaJson = objectMapper.writeValueAsString(schema);

        JobParameters params = new JobParametersBuilder()
                .addString("tableName", tableName)
                .addString("schema", schemaJson)
                .addLong("rowCount", (long) rowCount)
                .addLong("seed", seed)
                .addLong("timestamp", System.currentTimeMillis()) // 유니크 실행 보장
                .toJobParameters();

        log.info("Launching batch job - table: {}, rows: {}", tableName, rowCount);

        JobExecution execution = jobLauncher.run(dataGenerationJob, params);

        log.info("Batch job started - executionId: {}, status: {}",
                execution.getId(), execution.getStatus());

        return execution;
    }

    /**
     * Job 실행 상태 조회
     */
    public JobExecutionStatus getJobStatus(Long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);

        if (execution == null) {
            return JobExecutionStatus.builder()
                    .executionId(executionId)
                    .status("NOT_FOUND")
                    .message("Job execution not found")
                    .build();
        }

        // Step 진행 상황 가져오기
        long readCount = 0;
        long writeCount = 0;
        for (StepExecution stepExecution : execution.getStepExecutions()) {
            if ("dataGenerationStep".equals(stepExecution.getStepName())) {
                readCount = stepExecution.getReadCount();
                writeCount = stepExecution.getWriteCount();
            }
        }

        return JobExecutionStatus.builder()
                .executionId(executionId)
                .status(execution.getStatus().name())
                .exitStatus(execution.getExitStatus().getExitCode())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .readCount(readCount)
                .writeCount(writeCount)
                .message(getStatusMessage(execution))
                .isComplete(execution.getStatus().isUnsuccessful() ||
                        execution.getStatus() == BatchStatus.COMPLETED)
                .build();
    }

    /**
     * 진행중인 Job 목록 조회
     */
    public java.util.List<JobExecutionStatus> getRunningJobs() {
        return jobExplorer.findRunningJobExecutions("dataGenerationJob").stream()
                .map(execution -> getJobStatus(execution.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    private String getStatusMessage(JobExecution execution) {
        switch (execution.getStatus()) {
            case STARTING:
                return "Job is starting...";
            case STARTED:
                return "Job is running...";
            case COMPLETED:
                return "Job completed successfully";
            case FAILED:
                return "Job failed: " + execution.getExitStatus().getExitDescription();
            case STOPPED:
                return "Job was stopped";
            default:
                return execution.getStatus().name();
        }
    }

    /**
     * Job 실행 상태 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JobExecutionStatus {
        private Long executionId;
        private String status;
        private String exitStatus;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private long readCount;
        private long writeCount;
        private String message;
        private boolean isComplete;
    }
}
