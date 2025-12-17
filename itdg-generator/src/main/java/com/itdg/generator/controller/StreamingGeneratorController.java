package com.itdg.generator.controller;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.service.StreamingDataGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 스트리밍 기반 데이터 생성 API
 * 
 * 특징:
 * - StreamingResponseBody로 생성 즉시 클라이언트에 전송
 * - 메모리 O(1) - 대용량 처리 가능
 * - CSV, XLSX, JSON 포맷 지원
 */
@Slf4j
@RestController
@RequestMapping("/api/generator/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StreamingGeneratorController {

    private final StreamingDataGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    /**
     * CSV 스트리밍 다운로드
     * 생성 즉시 클라이언트로 전송 → 메모리 O(1)
     */
    @PostMapping("/csv")
    public ResponseEntity<StreamingResponseBody> streamCsv(
            @RequestBody TableMetadata table,
            @RequestParam(defaultValue = "1000") int rowCount,
            @RequestParam(defaultValue = "0") long seed) {

        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        String tableName = table.getTableName();

        log.info("Starting CSV streaming for table: {}, rows: {}", tableName, rowCount);

        StreamingResponseBody body = outputStream -> {
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                // BOM for Excel compatibility
                writer.write('\uFEFF');

                // 1. 헤더 쓰기
                List<String> columnNames = table.getColumns().stream()
                        .map(ColumnMetadata::getName)
                        .collect(Collectors.toList());
                writer.println(String.join(",", columnNames));
                writer.flush();

                // 2. 데이터 스트리밍 - 생성 즉시 쓰기
                AtomicInteger count = new AtomicInteger(0);
                generatorService.generateDataStream(table, rowCount, actualSeed)
                        .forEach(row -> {
                            String csvLine = toCsvLine(row, columnNames);
                            writer.println(csvLine);

                            // 1000개마다 flush → 클라이언트에 즉시 전달
                            int current = count.incrementAndGet();
                            if (current % 1000 == 0) {
                                writer.flush();
                                log.debug("Streamed {} rows", current);
                            }
                        });

                log.info("CSV streaming completed: {} rows", count.get());
            } catch (Exception e) {
                log.error("Error during CSV streaming", e);
                throw new RuntimeException(e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + tableName + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    /**
     * XLSX 스트리밍 다운로드
     * SXSSFWorkbook 사용으로 메모리 효율적 처리
     */
    @PostMapping("/xlsx")
    public ResponseEntity<StreamingResponseBody> streamXlsx(
            @RequestBody TableMetadata table,
            @RequestParam(defaultValue = "1000") int rowCount,
            @RequestParam(defaultValue = "0") long seed) {

        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        String tableName = table.getTableName();

        log.info("Starting XLSX streaming for table: {}, rows: {}", tableName, rowCount);

        StreamingResponseBody body = outputStream -> {
            // SXSSFWorkbook: 메모리에 100행만 유지, 나머지는 디스크에 임시 저장
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                Sheet sheet = workbook.createSheet(tableName);

                List<String> columnNames = table.getColumns().stream()
                        .map(ColumnMetadata::getName)
                        .collect(Collectors.toList());

                // 헤더 스타일
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // 헤더 행 생성
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columnNames.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columnNames.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // 데이터 스트리밍
                AtomicInteger rowNum = new AtomicInteger(1);
                generatorService.generateDataStream(table, rowCount, actualSeed)
                        .forEach(data -> {
                            Row row = sheet.createRow(rowNum.getAndIncrement());
                            for (int i = 0; i < columnNames.size(); i++) {
                                Cell cell = row.createCell(i);
                                setCellValue(cell, data.get(columnNames.get(i)));
                            }
                        });

                workbook.write(outputStream);
                workbook.dispose(); // 임시 파일 정리

                log.info("XLSX streaming completed: {} rows", rowNum.get() - 1);
            } catch (Exception e) {
                log.error("Error during XLSX streaming", e);
                throw new RuntimeException(e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + tableName + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /**
     * JSON 배열 다운로드
     */
    @PostMapping("/json")
    public ResponseEntity<StreamingResponseBody> streamJson(
            @RequestBody TableMetadata table,
            @RequestParam(defaultValue = "1000") int rowCount,
            @RequestParam(defaultValue = "0") long seed) {

        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        String tableName = table.getTableName();

        log.info("Starting JSON streaming for table: {}, rows: {}", tableName, rowCount);

        StreamingResponseBody body = outputStream -> {
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

                writer.print("[");
                AtomicInteger count = new AtomicInteger(0);

                generatorService.generateDataStream(table, rowCount, actualSeed)
                        .forEach(row -> {
                            try {
                                if (count.getAndIncrement() > 0) {
                                    writer.print(",\n");
                                }
                                writer.print(objectMapper.writeValueAsString(row));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                writer.print("]");
                writer.flush();

                log.info("JSON streaming completed: {} rows", count.get());
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + tableName + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * NDJSON 스트리밍 (API 간 통신용)
     */
    @PostMapping(value = "/ndjson", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> streamNdjson(
            @RequestBody TableMetadata table,
            @RequestParam(defaultValue = "1000") int rowCount,
            @RequestParam(defaultValue = "0") long seed) {

        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        String tableName = table.getTableName();

        log.info("Starting NDJSON streaming for table: {}, rows: {}", tableName, rowCount);

        StreamingResponseBody body = outputStream -> {
            try (BufferedOutputStream buffered = new BufferedOutputStream(outputStream, 8192)) {
                AtomicInteger count = new AtomicInteger(0);

                generatorService.generateDataStream(table, rowCount, actualSeed)
                        .forEach(row -> {
                            try {
                                buffered.write(objectMapper.writeValueAsBytes(row));
                                buffered.write('\n');

                                int current = count.incrementAndGet();
                                if (current % 1000 == 0) {
                                    buffered.flush();
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                buffered.flush();
                log.info("NDJSON streaming completed: {} rows", count.get());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(body);
    }

    /**
     * Row를 CSV 라인으로 변환
     */
    private String toCsvLine(Map<String, Object> row, List<String> columnNames) {
        return columnNames.stream()
                .map(col -> {
                    Object value = row.get(col);
                    if (value == null)
                        return "";
                    String str = value.toString();
                    // CSV 이스케이프: 쉼표, 따옴표, 줄바꿈 포함 시
                    if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                        str = "\"" + str.replace("\"", "\"\"") + "\"";
                    }
                    return str;
                })
                .collect(Collectors.joining(","));
    }

    /**
     * Excel 셀 값 설정 (타입별 처리)
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
