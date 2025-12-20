package com.itdg.generator.controller;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.service.StreamingDataGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 스트리밍 기반 데이터 생성 API (복구됨)
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

                writer.write('\uFEFF'); // BOM

                List<String> columnNames = table.getColumns().stream()
                        .map(ColumnMetadata::getName)
                        .collect(Collectors.toList());
                writer.println(String.join(",", columnNames));
                writer.flush();

                AtomicInteger count = new AtomicInteger(0);
                generatorService.generateDataStream(table, rowCount, actualSeed)
                        .forEach(row -> {
                            String csvLine = toCsvLine(row, columnNames);
                            writer.println(csvLine);

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

    private String toCsvLine(Map<String, Object> row, List<String> columnNames) {
        return columnNames.stream()
                .map(col -> {
                    Object value = row.get(col);
                    if (value == null)
                        return "";
                    String str = value.toString();
                    if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                        str = "\"" + str.replace("\"", "\"\"") + "\"";
                    }
                    return str;
                })
                .collect(Collectors.joining(","));
    }
}
