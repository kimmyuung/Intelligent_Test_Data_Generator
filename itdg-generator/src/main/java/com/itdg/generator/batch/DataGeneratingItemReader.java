package com.itdg.generator.batch;

import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.generator.service.StreamingDataGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generator Service로부터 스트림으로 데이터를 읽어오는 커스텀 Reader
 * 
 * 메모리 효율: O(1) - 한 번에 한 Row만 메모리에 존재
 * Iterator 기반으로 필요한 만큼만 생성
 */
@Slf4j
public class DataGeneratingItemReader implements ItemReader<Map<String, Object>> {

    private final Iterator<Map<String, Object>> dataIterator;
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final int totalRows;
    private final String tableName;

    public DataGeneratingItemReader(
            StreamingDataGeneratorService generatorService,
            TableMetadata tableMetadata,
            int totalRows,
            long seed) {

        this.totalRows = totalRows;
        this.tableName = tableMetadata.getTableName();

        // Stream을 Iterator로 변환 → 하나씩 읽기
        this.dataIterator = generatorService
                .generateDataStream(tableMetadata, totalRows, seed)
                .iterator();

        log.info("DataGeneratingItemReader initialized for table: {}, totalRows: {}",
                tableName, totalRows);
    }

    @Override
    public Map<String, Object> read() throws Exception {
        if (dataIterator.hasNext()) {
            Map<String, Object> row = dataIterator.next();

            int count = readCount.incrementAndGet();

            // 10000개마다 진행 상황 로깅
            if (count % 10000 == 0) {
                int percent = (int) ((count * 100L) / totalRows);
                log.info("[{}] Read progress: {} / {} rows ({}%)",
                        tableName, count, totalRows, percent);
            }

            return row;
        }

        log.info("[{}] Read completed: {} rows", tableName, readCount.get());
        return null; // 스트림 종료 신호 → Step 종료
    }

    public int getReadCount() {
        return readCount.get();
    }

    public int getTotalRows() {
        return totalRows;
    }
}
