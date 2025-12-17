package com.itdg.generator.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * JDBC Batch Insert Writer
 * 
 * 청크 단위(기본 5000개)로 묶어서 한 번에 INSERT
 * → 네트워크 왕복 최소화 → 속도 10배 향상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcBatchInsertWriter implements ItemWriter<Map<String, Object>> {

    private final JdbcTemplate jdbcTemplate;

    private String tableName;
    private List<String> columns;
    private final AtomicLong totalInserted = new AtomicLong(0);

    /**
     * 테이블 정보 설정 (Step 시작 전 호출)
     */
    public void setTableInfo(String tableName, List<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
        this.totalInserted.set(0);
        log.info("JdbcBatchInsertWriter configured for table: {}, columns: {}",
                tableName, columns.size());
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        if (items.isEmpty())
            return;
        if (tableName == null || columns == null) {
            throw new IllegalStateException("Table info not set. Call setTableInfo() first.");
        }

        String sql = buildInsertSql();

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map<String, Object> row = items.get(i);
                int paramIndex = 1;

                for (String col : columns) {
                    Object value = row.get(col);
                    setPreparedStatementValue(ps, paramIndex++, value);
                }
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        long total = totalInserted.addAndGet(items.size());
        log.debug("Batch inserted {} rows into {} (total: {})", items.size(), tableName, total);
    }

    /**
     * INSERT SQL 생성
     */
    private String buildInsertSql() {
        String cols = String.join(", ", columns);
        String placeholders = columns.stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, cols, placeholders);
    }

    /**
     * PreparedStatement 값 설정 (타입별 처리)
     */
    private void setPreparedStatementValue(PreparedStatement ps, int index, Object value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.NULL);
        } else if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            ps.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            ps.setFloat(index, (Float) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        } else if (value instanceof LocalDateTime) {
            ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof java.util.Date) {
            ps.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
        } else {
            ps.setObject(index, value);
        }
    }

    public long getTotalInserted() {
        return totalInserted.get();
    }
}
