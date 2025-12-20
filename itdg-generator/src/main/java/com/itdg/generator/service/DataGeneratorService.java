package com.itdg.generator.service;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.generator.constraint.UniqueValueTracker;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGeneratorService {

    private final List<DataGeneratorStrategy> strategies;

    /**
     * 전체 데이터 생성 (동기식, 인메모리 수집)
     * 소규모 데이터 생성에 적합
     */
    public GenerateDataResponse generateData(GenerateDataRequest request) {
        log.info("Starting data generation for request with seed: {}", request.getSeed());

        long seed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();
        Map<String, List<Map<String, Object>>> successData = new HashMap<>();
        Map<String, Integer> statistics = new HashMap<>();

        // request.getSchema() null check
        if (request.getSchema() == null || request.getSchema().getTables() == null) {
            return GenerateDataResponse.builder()
                    .success(false)
                    .message("Schema or tables cannot be null")
                    .build();
        }

        for (TableMetadata table : request.getSchema().getTables()) {
            log.info("Generating data for table: {}", table.getTableName());
            int rowCount = request.getRowCount() != null ? request.getRowCount() : 100;

            // 스트림을 리스트로 수집
            List<Map<String, Object>> rows = generateDataStream(table, rowCount, seed)
                    .collect(Collectors.toList());

            successData.put(table.getTableName(), rows);
            statistics.put(table.getTableName(), rows.size());
        }

        return GenerateDataResponse.builder()
                .generatedData(successData)
                .statistics(statistics)
                .generatedAt(LocalDateTime.now())
                .seed(seed)
                .success(true)
                .message("Successfully generated data for " + successData.size() + " tables")
                .build();
    }

    /**
     * 대용량 처리를 위한 Stream 기반 데이터 생성
     * 메모리 효율적 (O(1))
     */
    public Stream<Map<String, Object>> generateDataStream(
            TableMetadata table, int rowCount, long seed) {

        Random random = new Random(seed);
        UniqueValueTracker uniqueTracker = new UniqueValueTracker();
        AtomicLong pkSequence = new AtomicLong(1);

        return IntStream.range(0, rowCount)
                .mapToObj(i -> generateRow(table, random, uniqueTracker, pkSequence));
    }

    private Map<String, Object> generateRow(TableMetadata table, Random random, UniqueValueTracker uniqueTracker,
            AtomicLong pkSequence) {
        Map<String, Object> row = new HashMap<>();
        int maxRetries = 5;

        for (ColumnMetadata column : table.getColumns()) {
            Object value = null;
            boolean valid = false;

            // 1. Primary Key Handling
            if (Boolean.TRUE.equals(column.getIsPrimaryKey())) {
                value = generatePrimaryKey(column, pkSequence);
                valid = true;
            }
            // 2. Foreign Key Handling (Mock)
            else if (Boolean.TRUE.equals(column.getIsForeignKey())) {
                // Simulate referring to an existing ID from another table
                // Assuming target table has at least 10 rows
                value = random.nextInt(10) + 1; // 1 to 10
                valid = true;
            } else {
                // 3. Normal Column Generation with Retry for Unique Constraints
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    value = generateColumnValue(column, random);

                    // Not Null Check
                    if (!Boolean.TRUE.equals(column.getIsNullable()) && value == null) {
                        continue; // Try again
                    }

                    valid = true;
                    break;
                }
            }

            if (!valid && !Boolean.TRUE.equals(column.getIsNullable())) {
                log.warn("Failed to generate valid value for NOT NULL column: {}.{}", table.getTableName(),
                        column.getName());
                value = getDefaultValue(column); // Fallback
            }

            row.put(column.getName(), value);
        }
        return row;
    }

    // Fallback for failed generation
    private Object getDefaultValue(ColumnMetadata column) {
        String type = column.getDataType().toUpperCase();
        if (type.contains("INT") || type.contains("NUMBER"))
            return 0;
        if (type.contains("STR") || type.contains("CHAR"))
            return "DEFAULT";
        if (type.contains("DATE"))
            return "2024-01-01";
        return "fallback";
    }

    private Object generatePrimaryKey(ColumnMetadata column, AtomicLong pkSequence) {
        String type = column.getDataType().toUpperCase();
        if (Boolean.TRUE.equals(column.getIsAutoIncrement()) || type.contains("INT") || type.contains("SERIAL")) {
            return pkSequence.getAndIncrement();
        }
        return UUID.randomUUID().toString();
    }

    private Object generateColumnValue(ColumnMetadata column, Random random) {
        // Strategy Pattern Application
        for (DataGeneratorStrategy strategy : strategies) {
            if (strategy.supports(column)) {
                return strategy.generate(column, random);
            }
        }
        // Fallback if no strategy supports
        return "N/A";
    }
}
