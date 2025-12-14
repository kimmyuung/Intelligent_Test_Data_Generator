package com.itdg.analyzer.service;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MetadataExtractor {

    public List<TableMetadata> extractTables(Connection connection, String databaseName) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        String[] types = { "TABLE" };

        try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableSchema = rs.getString("TABLE_SCHEM");

                if (isSystemTable(tableSchema, tableName)) {
                    continue;
                }

                log.debug("Analyzing table: {}", tableName);

                List<String> primaryKeys = extractPrimaryKeys(metaData, tableName);
                List<ColumnMetadata> rawColumns = extractColumns(metaData, tableName);

                // PK 여부 반영하여 컬럼 리스트 재생성
                List<ColumnMetadata> columns = rawColumns.stream()
                        .map(col -> {
                            boolean isPk = primaryKeys.contains(col.getName());
                            return ColumnMetadata.builder()
                                    .name(col.getName())
                                    .dataType(col.getDataType())
                                    .length(col.getLength())
                                    .isNullable(col.getIsNullable())
                                    .isAutoIncrement(col.getIsAutoIncrement())
                                    .isPrimaryKey(isPk)
                                    .comment(col.getComment())
                                    .build();
                        })
                        .collect(Collectors.toList());

                TableMetadata tableMetadata = TableMetadata.builder()
                        .tableName(tableName)
                        .columns(columns)
                        .primaryKeys(primaryKeys)
                        .rowCount(estimateRowCount(connection, tableName))
                        .build();

                tables.add(tableMetadata);
            }
        }

        return tables;
    }

    private List<ColumnMetadata> extractColumns(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();

        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int nullable = rs.getInt("NULLABLE");
                String remarks = rs.getString("REMARKS");

                String isAutoIncrementStr = "";
                try {
                    isAutoIncrementStr = rs.getString("IS_AUTOINCREMENT");
                } catch (SQLException e) {
                    log.warn("AutoIncrement check not supported for driver");
                }

                boolean isNullable = (nullable == DatabaseMetaData.columnNullable);
                boolean isAutoIncrement = "YES".equalsIgnoreCase(isAutoIncrementStr);

                ColumnMetadata column = ColumnMetadata.builder()
                        .name(columnName)
                        .dataType(typeName)
                        .length(columnSize)
                        .isNullable(isNullable)
                        .isAutoIncrement(isAutoIncrement)
                        .isPrimaryKey(false) // extractTables에서 처리
                        .comment(remarks)
                        .build();

                columns.add(column);
            }
        }
        return columns;
    }

    private List<String> extractPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    private boolean isSystemTable(String schema, String tableName) {
        if (schema == null)
            return false;
        // PostgreSQL system schemas + others
        return "information_schema".equalsIgnoreCase(schema) ||
                "pg_catalog".equalsIgnoreCase(schema) ||
                schema.startsWith("pg_toast");
    }

    private Long estimateRowCount(Connection connection, String tableName) {
        // 간단한 count 쿼리 실행. 실제 운영 DB에서는 조심해야 할 수 있음.
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.warn("Failed to count rows for table {}: {}", tableName, e.getMessage());
        }
        return 0L;
    }
}
