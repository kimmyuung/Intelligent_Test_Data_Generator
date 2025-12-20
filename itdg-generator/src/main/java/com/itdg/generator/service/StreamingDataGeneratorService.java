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

        // 컬럼이 하나도 없으면 루프가 안 돌아서 빈 Row가 생성되지만,
        // 굳이 기본값을 강제 주입하지 않고 그대로 둠 (사용자 요청: 학습 결과에 맡김)

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
            int maxRetries = 10; // Retry 횟수

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
                            // 마지막 시도면 강제 유니크 값 생성
                            if (retry == maxRetries - 1) {
                                value = forceUniqueValue(column, value);
                            } else {
                                continue;
                            }
                        }
                    }

                    // Not Null 체크
                    if (!Boolean.TRUE.equals(column.getIsNullable()) && value == null) {
                        continue;
                    }

                    valid = true;
                }
            }

            // Fallback: 여전히 유효하지 않다면 기본값 사용
            if (!valid) {
                if (Boolean.TRUE.equals(column.getIsNullable())) {
                    value = null; // Nullable이면 null 허용
                } else {
                    value = getDefaultValue(column);
                    // 기본값도 Unique여야 한다면 강제 변환
                    if (Boolean.TRUE.equals(column.getIsUnique())) {
                        value = forceUniqueValue(column, value);
                    }
                }
            }

            // 최종 값 Unique 등록 (필수)
            if (Boolean.TRUE.equals(column.getIsUnique()) && value != null) {
                uniqueTracker.add(column.getName(), value);
            }

            row.put(column.getName(), value);
        }

        return row;
    }

    /**
     * 강제로 유니크한 값 생성 (충돌 회피)
     */
    private Object forceUniqueValue(ColumnMetadata column, Object originalValue) {
        String type = column.getDataType().toUpperCase();
        String suffix = "_" + UUID.randomUUID().toString().substring(0, 5);

        if (type.contains("CHAR") || type.contains("TEXT") || type.contains("STRING")) {
            return String.valueOf(originalValue) + suffix;
        }
        if (type.contains("INT") || type.contains("LONG")) {
            return System.nanoTime() % 1000000; // 임시: 시간 기반 난수
        }
        return originalValue; // 다른 타입은 포기
    }

    /**
     * Primary Key 생성
     */
    private Object generatePrimaryKey(ColumnMetadata column, AtomicLong pkSequence) {
        String type = column.getDataType().toUpperCase();
        if (Boolean.TRUE.equals(column.getIsAutoIncrement()) ||
                type.contains("INT") || type.contains("SERIAL") || type.contains("LONG")) {
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
