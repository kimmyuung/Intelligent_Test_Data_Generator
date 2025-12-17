package com.itdg.generator.batch;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.service.StreamingDataGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring Batch 기반 대용량 데이터 생성 및 DB 삽입 설정
 * 
 * 특징:
 * - Chunk 기반 처리 (기본 5000개)
 * - ItemReader: 스트리밍 생성 (메모리 O(1))
 * - ItemWriter: JDBC Batch Insert (속도 최적화)
 * - 장애 복구: 마지막 청크부터 재시작 가능
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class DataGenerationBatchConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final StreamingDataGeneratorService generatorService;
    private final JdbcBatchInsertWriter jdbcWriter;
    private final ObjectMapper objectMapper;

    private static final int CHUNK_SIZE = 5000;

    /**
     * 대용량 데이터 생성 및 삽입 Job
     */
    @Bean
    public Job dataGenerationJob() {
        return jobBuilderFactory.get("dataGenerationJob")
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .start(initializationStep())
                .next(dataGenerationStep(null, null, 0, 0))
                .build();
    }

    /**
     * Job 실행 리스너
     */
    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            private long startTime;

            @Override
            public void beforeJob(JobExecution jobExecution) {
                startTime = System.currentTimeMillis();
                log.info("========================================");
                log.info("Starting Data Generation Job");
                log.info("Job Parameters: {}", jobExecution.getJobParameters());
                log.info("========================================");
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("========================================");
                log.info("Job Completed: {}", jobExecution.getStatus());
                log.info("Duration: {}ms ({} seconds)", duration, duration / 1000);
                log.info("========================================");
            }
        };
    }

    /**
     * 초기화 Step (테이블 검증 등)
     */
    @Bean
    public Step initializationStep() {
        return stepBuilderFactory.get("initializationStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("Initializing data generation...");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * Chunk 기반 스트리밍 데이터 생성 Step
     * 
     * - Reader: Iterator 기반 1개씩 생성 (메모리 O(1))
     * - Writer: 5000개씩 Batch Insert (속도 최적화)
     */
    @Bean
    @StepScope
    public Step dataGenerationStep(
            @Value("#{jobParameters['tableName']}") String tableName,
            @Value("#{jobParameters['schema']}") String schemaJson,
            @Value("#{jobParameters['rowCount']}") Integer rowCount,
            @Value("#{jobParameters['seed']}") Long seed) {

        // Null 체크 (Bean 생성 시점에는 파라미터가 null일 수 있음)
        if (tableName == null || schemaJson == null) {
            return stepBuilderFactory.get("dataGenerationStep")
                    .tasklet((contribution, chunkContext) -> {
                        log.warn("Step parameters not provided, skipping...");
                        return RepeatStatus.FINISHED;
                    })
                    .build();
        }

        TableMetadata table = parseSchema(schemaJson);
        List<String> columnNames = table.getColumns().stream()
                .map(ColumnMetadata::getName)
                .collect(Collectors.toList());

        // Writer 설정
        jdbcWriter.setTableInfo(tableName, columnNames);

        int finalRowCount = rowCount != null ? rowCount : 1000;
        long finalSeed = seed != null ? seed : System.currentTimeMillis();

        return stepBuilderFactory.get("dataGenerationStep")
                .<Map<String, Object>, Map<String, Object>>chunk(CHUNK_SIZE)
                .reader(new DataGeneratingItemReader(generatorService, table, finalRowCount, finalSeed))
                .processor(passThroughProcessor())
                .writer(jdbcWriter)
                .faultTolerant()
                .skipLimit(100) // 최대 100개 오류 허용
                .skip(DataAccessException.class)
                .listener(chunkListener(finalRowCount))
                .listener(stepExecutionListener())
                .build();
    }

    /**
     * Pass-through Processor (변환 없이 그대로 전달)
     * 필요시 데이터 변환 로직 추가 가능
     */
    @Bean
    public ItemProcessor<Map<String, Object>, Map<String, Object>> passThroughProcessor() {
        return item -> item;
    }

    /**
     * Chunk 처리 리스너 (진행률 로깅)
     */
    private ChunkListener chunkListener(int totalRows) {
        return new ChunkListener() {
            @Override
            public void afterChunk(ChunkContext context) {
                long readCount = context.getStepContext()
                        .getStepExecution().getReadCount();
                int percent = (int) ((readCount * 100L) / totalRows);

                if (readCount % (CHUNK_SIZE * 2) == 0) {
                    log.info("Progress: {} / {} rows ({}%)", readCount, totalRows, percent);
                }
            }

            @Override
            public void beforeChunk(ChunkContext context) {
            }

            @Override
            public void afterChunkError(ChunkContext context) {
                log.error("Chunk error occurred at read count: {}",
                        context.getStepContext().getStepExecution().getReadCount());
            }
        };
    }

    /**
     * Step 실행 리스너
     */
    private StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("Starting step: {}", stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("Step completed: {} - Read: {}, Write: {}, Skip: {}",
                        stepExecution.getStepName(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }

    /**
     * JSON → TableMetadata 파싱
     */
    private TableMetadata parseSchema(String schemaJson) {
        try {
            return objectMapper.readValue(schemaJson, TableMetadata.class);
        } catch (Exception e) {
            log.error("Failed to parse schema JSON", e);
            throw new RuntimeException("Invalid schema JSON", e);
        }
    }
}
