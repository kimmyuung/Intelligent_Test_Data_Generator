package com.itdg.analyzer.service;

import com.itdg.analyzer.dto.request.DbConnectionRequest;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaAnalyzerService {

    private final MetadataExtractor metadataExtractor;

    public ApiResponse<SchemaMetadata> analyze(DbConnectionRequest request) {
        log.info("Starting schema analysis for URL: {}", request.getUrl());

        try {
            // 드라이버 로드 (필요한 경우)
            if (request.getDriverClassName() != null && !request.getDriverClassName().isEmpty()) {
                Class.forName(request.getDriverClassName());
            }

            try (Connection connection = DriverManager.getConnection(
                    request.getUrl(), request.getUsername(), request.getPassword())) {

                List<TableMetadata> tables = metadataExtractor.extractTables(connection, null);

                SchemaMetadata schemaMetadata = SchemaMetadata.builder()
                        .databaseName(extractDatabaseName(request.getUrl()))
                        .tables(tables)
                        .analyzedAt(LocalDateTime.now())
                        .build();

                log.info("Analysis completed. Found {} tables.", tables.size());
                return ApiResponse.success(schemaMetadata);

            } catch (SQLException e) {
                log.error("Database connection error", e);
                return ApiResponse.error("Failed to connect to database: " + e.getMessage());
            }

        } catch (ClassNotFoundException e) {
            log.error("Driver class not found: {}", request.getDriverClassName(), e);
            return ApiResponse.error("Driver class not found: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during analysis", e);
            return ApiResponse.error("An unexpected error occurred: " + e.getMessage());
        }
    }

    private String extractDatabaseName(String url) {
        // jdbc:postgresql://localhost:5432/itdg -> itdg
        try {
            int lastSlash = url.lastIndexOf('/');
            int questionMark = url.indexOf('?');
            if (questionMark == -1) {
                return url.substring(lastSlash + 1);
            }
            return url.substring(lastSlash + 1, questionMark);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
