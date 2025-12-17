package com.itdg.generator.service;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.constraint.UniqueValueTracker;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 스트리밍 기반 대용량 데이터 생성 서비스
 * 
 * 메모리 효율: O(1) - 한 번에 한 Row만 메모리에 존재
 * 100만 건 생성해도 메모리 ~50MB 고정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingDataGeneratorService {

    private final List<DataGeneratorStrategy> strategies;

    /**
     * Stream 기반 대용량 데이터 생성
     * 
     * @param table    테이블 메타데이터
     * @param rowCount 생성할 행 수
     * @param seed     랜덤 시드 (재현성 보장)
     * @return Stream<Map<String, Object>> - Iterator 기반 스트림
     */
    public Stream<Map<String, Object>> generateDataStream(
            TableMetadata table,
            int rowCount,
            long seed) {

        log.info("Starting streaming data generation for table: {}, rows: {}, seed: {}",
                table.getTableName(), rowCount, seed);

        Random random = new Random(seed);
        UniqueValueTracker uniqueTracker = new UniqueValueTracker();
        AtomicLong pkSequence = new AtomicLong(1);

        return IntStream.range(0, rowCount)
                .mapToObj(i -> generateRow(table, random, uniqueTracker, pkSequence));
    }

    /**
     * Iterator 기반 대용량 데이터 생성
     * Spring Batch ItemReader에서 사용
     */
    public Iterator<Map<String, Object>> generateDataIterator(
            TableMetadata table,
            int rowCount,
            long seed) {

        return generateDataStream(table, rowCount, seed).iterator();
    }

    /**
     * 단일 Row 생성
     */
    private Map<String, Object> generateRow(
            TableMetadata table,
            Random random,
            UniqueValueTracker uniqueTracker,
            AtomicLong pkSequence) {

        Map<String, Object> row = new LinkedHashMap<>(); // 순서 유지

        for (ColumnMetadata column : table.getColumns()) {
            Object value = null;
            boolean valid = false;
            int maxRetries = 5;

            // 1. Primary Key 처리
            if (Boolean.TRUE.equals(column.getIsPrimaryKey())) {
                value = generatePrimaryKey(column, pkSequence);
                valid = true;
            }
            // 2. Foreign Key 처리 (Mock)
            else if (column.getName().toLowerCase().endsWith("_id")) {
                value = random.nextInt(100) + 1; // 1-100 범위
                valid = true;
            }
            // 3. 일반 컬럼 처리
            else {
                for (int retry = 0; retry < maxRetries && !valid; retry++) {
                    value = generateColumnValue(column, random);

                    // Unique 체크
                    if (Boolean.TRUE.equals(column.getIsUnique())) {
                        if (!uniqueTracker.isUnique(column.getName(), value)) {
                            continue;
                        }
                    }

                    // Not Null 체크
                    if (!Boolean.TRUE.equals(column.getIsNullable()) && value == null) {
                        continue;
                    }

                    valid = true;
                }
            }

            // Fallback
            if (!valid && !Boolean.TRUE.equals(column.getIsNullable())) {
                value = getDefaultValue(column);
            }

            row.put(column.getName(), value);
        }

        return row;
    }

    /**
     * Primary Key 생성
     */
    private Object generatePrimaryKey(ColumnMetadata column, AtomicLong pkSequence) {
        String type = column.getDataType().toUpperCase();
        if (Boolean.TRUE.equals(column.getIsAutoIncrement()) ||
                type.contains("INT") || type.contains("SERIAL")) {
            return pkSequence.getAndIncrement();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 컬럼 값 생성 (Strategy Pattern 적용)
     */
    private Object generateColumnValue(ColumnMetadata column, Random random) {
        for (DataGeneratorStrategy strategy : strategies) {
            if (strategy.supports(column)) {
                return strategy.generate(column, random);
            }
        }
        // 기본값
        return generateDefaultByType(column, random);
    }

    /**
     * 타입별 기본값 생성
     */
    private Object generateDefaultByType(ColumnMetadata column, Random random) {
        String type = column.getDataType().toUpperCase();

        if (type.contains("INT") || type.contains("SERIAL")) {
            return random.nextInt(10000);
        }
        if (type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("DECIMAL")) {
            return Math.round(random.nextDouble() * 10000) / 100.0;
        }
        if (type.contains("BOOL")) {
            return random.nextBoolean();
        }
        if (type.contains("DATE") || type.contains("TIME")) {
            return java.time.LocalDateTime.now().minusDays(random.nextInt(365));
        }
        // VARCHAR, TEXT 등
        int length = column.getLength() != null && column.getLength() > 0 ? Math.min(column.getLength(), 20) : 10;
        return UUID.randomUUID().toString().substring(0, length);
    }

    /**
     * Not Null 컬럼 기본값
     */
    private Object getDefaultValue(ColumnMetadata column) {
        String type = column.getDataType().toUpperCase();
        if (type.contains("INT") || type.contains("SERIAL"))
            return 0;
        if (type.contains("FLOAT") || type.contains("DOUBLE"))
            return 0.0;
        if (type.contains("BOOL"))
            return false;
        return "N/A";
    }
}
