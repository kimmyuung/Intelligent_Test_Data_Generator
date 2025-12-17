# 대용량 처리를 위한 True Streaming 아키텍처

> 메모리 효율적인 대용량 테스트 데이터 생성 및 DB 삽입 구현 가이드

---

## 1. 현재 문제점 분석

### 1.1 기존 아키텍처의 한계

| 구간 | 현재 방식 | 문제점 |
|:---|:---|:---|
| **Generator** | 모든 데이터를 `List<Map>`에 저장 | 100만 건 생성 시 메모리 폭발 (OOM) |
| **Orchestrator** | 전체 데이터를 JSON으로 받음 | 네트워크 타임아웃, 메모리 문제 |
| **Frontend** | 전체 데이터를 한 번에 렌더링 | 브라우저 멈춤 |
| **DB Insert** | 전체 데이터 완성 후 삽입 | 장애 시 처음부터 재시작 |

### 1.2 메모리 사용량 비교

```
현재 방식 (100만 건 기준):
┌─────────────────────────────────────────────────────────────┐
│  Row 1 → List → Row 2 → List → ... → Row 1,000,000 → List  │
│                                                              │
│  메모리: ~2GB+  (모든 데이터가 메모리에 상주)                │
│  첫 응답: 데이터 생성 완료 후 (수 분 소요)                   │
└─────────────────────────────────────────────────────────────┘

True Streaming 방식:
┌─────────────────────────────────────────────────────────────┐
│  Row 1 → 즉시 전송 → Row 2 → 즉시 전송 → ...                │
│                                                              │
│  메모리: ~50MB (청크 크기만큼만 유지)                        │
│  첫 응답: 즉시 (~100ms)                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 개선 목표

1. **메모리 효율**: O(n) → O(1) 메모리 사용
2. **응답 속도**: 전체 완료 대기 → 즉시 스트리밍 시작
3. **장애 복구**: 처음부터 재시작 → 마지막 청크부터 재시작
4. **사용자 경험**: 대기 화면 → 실시간 진행률 표시

---

## 3. True Streaming 아키텍처 설계

### 3.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Complete True Streaming Architecture                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  [Frontend]                                                                  │
│      │                                                                       │
│      ├── SSE Progress ◄────────────────────────────────────────┐            │
│      │                                                          │            │
│      ▼                                                          │            │
│  [Orchestrator]                                                 │            │
│      │                                                          │            │
│      ├── 미리보기 → Flux<Map> ───► SSE ──────────────────────────┤            │
│      │                                                          │            │
│      ├── CSV 다운로드 → StreamingResponseBody ──► 브라우저 저장  │            │
│      │                                                          │            │
│      └── DB Insert → Spring Batch Job ─┐                        │            │
│                                        │                        │            │
│                                        ▼                        │            │
│  [Generator]                    [Batch Step]                    │            │
│      │                               │                          │            │
│      └── Stream<Map> ◄─────── ItemReader ──► ItemWriter ───► [DB]           │
│           (Iterator)          (1개씩 읽기)  (5000개 Batch)                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 기술 스택

| 계층 | 기술 | 역할 |
|:---|:---|:---|
| Generator | Java Stream / Iterator | 메모리 효율적 데이터 생성 |
| Orchestrator | Spring WebFlux Flux | 리액티브 스트리밍 중계 |
| API 응답 | StreamingResponseBody | 파일 다운로드 스트리밍 |
| 실시간 통신 | Server-Sent Events (SSE) | 진행률 실시간 전송 |
| DB 삽입 | Spring Batch | 청크 기반 배치 처리 |
| Frontend | EventSource | SSE 수신 및 표시 |

---

## 4. 구현 상세

### 4.1 Generator - Stream 기반 생성

#### StreamingDataGeneratorService.java

```java
@Service
public class StreamingDataGeneratorService {

    /**
     * 대용량 스트리밍 생성 - Iterator 기반
     * 메모리: O(1) - 한 번에 한 Row만 메모리에 존재
     */
    public Stream<Map<String, Object>> generateDataStream(
            TableMetadata table, 
            int rowCount, 
            long seed) {
        
        Random random = new Random(seed);
        UniqueValueTracker uniqueTracker = new UniqueValueTracker();
        AtomicLong pkSequence = new AtomicLong(1);
        
        return IntStream.range(0, rowCount)
            .mapToObj(i -> generateRow(table, random, uniqueTracker, pkSequence));
    }
    
    private Map<String, Object> generateRow(
            TableMetadata table, 
            Random random, 
            UniqueValueTracker uniqueTracker, 
            AtomicLong pkSequence) {
        // 기존 row 생성 로직
        Map<String, Object> row = new HashMap<>();
        for (ColumnMetadata column : table.getColumns()) {
            row.put(column.getName(), generateColumnValue(column, random, uniqueTracker, pkSequence));
        }
        return row;
    }
}
```

### 4.2 Generator - StreamingResponseBody Controller

#### StreamingGeneratorController.java

```java
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class StreamingGeneratorController {

    private final StreamingDataGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    /**
     * CSV 스트리밍 다운로드
     * 생성 즉시 클라이언트로 전송 → 메모리 O(1)
     */
    @GetMapping("/stream/csv")
    public ResponseEntity<StreamingResponseBody> streamCsv(
            @RequestParam String tableName,
            @RequestParam int rowCount,
            @RequestParam(defaultValue = "0") long seed) {
        
        TableMetadata table = getTableMetadata(tableName);
        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        
        StreamingResponseBody body = outputStream -> {
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                
                // 1. 헤더 쓰기
                String header = table.getColumns().stream()
                    .map(ColumnMetadata::getName)
                    .collect(Collectors.joining(","));
                writer.println(header);
                writer.flush();
                
                // 2. 데이터 스트리밍 - 생성 즉시 쓰기
                AtomicInteger count = new AtomicInteger(0);
                generatorService.generateDataStream(table, rowCount, actualSeed)
                    .forEach(row -> {
                        String csvLine = toCsvLine(row, table);
                        writer.println(csvLine);
                        
                        // 1000개마다 flush → 클라이언트에 즉시 전달
                        if (count.incrementAndGet() % 1000 == 0) {
                            writer.flush();
                        }
                    });
            }
        };
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + tableName + ".csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(body);
    }

    /**
     * NDJSON 스트리밍 (Newline Delimited JSON)
     * API 간 스트리밍 통신용
     */
    @GetMapping(value = "/stream/ndjson", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> streamNdjson(
            @RequestParam String tableName,
            @RequestParam int rowCount,
            @RequestParam(defaultValue = "0") long seed) {
        
        TableMetadata table = getTableMetadata(tableName);
        long actualSeed = seed == 0 ? System.currentTimeMillis() : seed;
        
        StreamingResponseBody body = outputStream -> {
            try (BufferedOutputStream buffered = new BufferedOutputStream(outputStream, 8192)) {
                generatorService.generateDataStream(table, rowCount, actualSeed)
                    .forEach(row -> {
                        try {
                            buffered.write(objectMapper.writeValueAsBytes(row));
                            buffered.write('\n');
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                buffered.flush();
            }
        };
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_NDJSON)
            .body(body);
    }
}
```

### 4.3 Orchestrator - Flux + SSE 중계

#### StreamingOrchestratorController.java

```java
@RestController
@RequestMapping("/api/orchestrator")
@RequiredArgsConstructor
public class StreamingOrchestratorController {

    private final WebClient generatorWebClient;

    /**
     * SSE 실시간 스트리밍 (프론트엔드용)
     * Generator → Orchestrator → Frontend 파이프라인
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DataChunk>> streamGenerate(
            @RequestParam String tableName,
            @RequestParam int rowCount) {
        
        AtomicInteger processed = new AtomicInteger(0);
        
        return generatorWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/generator/stream/ndjson")
                .queryParam("tableName", tableName)
                .queryParam("rowCount", rowCount)
                .build())
            .accept(MediaType.APPLICATION_NDJSON)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
            .buffer(100)  // 100개씩 묶어서 전송 (네트워크 효율)
            .map(rows -> {
                int current = processed.addAndGet(rows.size());
                return ServerSentEvent.<DataChunk>builder()
                    .id(String.valueOf(current))
                    .event("data")
                    .data(new DataChunk(rows, current, rowCount))
                    .build();
            })
            .concatWith(Flux.just(
                ServerSentEvent.<DataChunk>builder()
                    .event("complete")
                    .build()
            ))
            .doOnError(error -> log.error("Streaming error", error));
    }

    /**
     * 파일 다운로드 프록시 (스트리밍 유지)
     */
    @GetMapping("/download/csv")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadCsv(
            @RequestParam String tableName,
            @RequestParam int rowCount) {
        
        return Mono.just(
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + tableName + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(
                    generatorWebClient.get()
                        .uri("/api/generator/stream/csv?tableName=" + tableName + 
                             "&rowCount=" + rowCount)
                        .retrieve()
                        .bodyToFlux(DataBuffer.class)
                )
        );
    }

    @Data
    @AllArgsConstructor
    static class DataChunk {
        private List<Map<String, Object>> rows;
        private int progress;
        private int total;
    }
}
```

### 4.4 Spring Batch - 스트리밍 DB Insert

#### DataGeneratingItemReader.java

```java
/**
 * Generator Service로부터 스트림으로 데이터를 읽어오는 커스텀 Reader
 * 메모리 효율: 한 번에 한 Row만 메모리에 존재
 */
public class DataGeneratingItemReader implements ItemReader<Map<String, Object>> {

    private final Iterator<Map<String, Object>> dataIterator;
    private final AtomicInteger readCount = new AtomicInteger(0);
    private final int totalRows;
    
    public DataGeneratingItemReader(
            StreamingDataGeneratorService generatorService,
            TableMetadata tableMetadata,
            int totalRows,
            long seed) {
        
        this.totalRows = totalRows;
        // Stream을 Iterator로 변환 → 하나씩 읽기
        this.dataIterator = generatorService
            .generateDataStream(tableMetadata, totalRows, seed)
            .iterator();
    }
    
    @Override
    public Map<String, Object> read() throws Exception {
        if (dataIterator.hasNext()) {
            Map<String, Object> row = dataIterator.next();
            
            int count = readCount.incrementAndGet();
            if (count % 10000 == 0) {
                log.info("Read progress: {} / {} rows ({}%)", 
                    count, totalRows, (count * 100 / totalRows));
            }
            
            return row;
        }
        return null;  // 스트림 종료 신호
    }
}
```

#### JdbcBatchInsertWriter.java

```java
/**
 * JDBC Batch Insert Writer
 * 5000개씩 묶어서 한 번에 INSERT → 네트워크 왕복 최소화
 */
@Component
@RequiredArgsConstructor
public class JdbcBatchInsertWriter implements ItemWriter<Map<String, Object>> {

    private final JdbcTemplate jdbcTemplate;
    
    private String tableName;
    private List<String> columns;

    public void setTableInfo(String tableName, List<String> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        if (items.isEmpty()) return;

        String sql = buildInsertSql();
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map<String, Object> row = items.get(i);
                int paramIndex = 1;
                for (String col : columns) {
                    ps.setObject(paramIndex++, row.get(col));
                }
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
        
        log.debug("Batch inserted {} rows into {}", items.size(), tableName);
    }

    private String buildInsertSql() {
        String cols = String.join(", ", columns);
        String placeholders = columns.stream()
            .map(c -> "?")
            .collect(Collectors.joining(", "));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", 
            tableName, cols, placeholders);
    }
}
```

#### DataGenerationBatchConfig.java

```java
@Configuration
@EnableBatchProcessing
public class DataGenerationBatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private StreamingDataGeneratorService generatorService;

    @Autowired
    private JdbcBatchInsertWriter jdbcWriter;

    private static final int CHUNK_SIZE = 5000;

    /**
     * 대용량 데이터 생성 및 삽입 Job
     */
    @Bean
    public Job dataGenerationJob() {
        return jobBuilderFactory.get("dataGenerationJob")
            .incrementer(new RunIdIncrementer())
            .listener(jobExecutionListener())
            .start(dataGenerationStep(null, null, 0, 0))
            .build();
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            private long startTime;
            
            @Override
            public void beforeJob(JobExecution jobExecution) {
                startTime = System.currentTimeMillis();
                log.info("=== Starting data generation job ===");
            }
            
            @Override
            public void afterJob(JobExecution jobExecution) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("=== Job completed: {} in {}ms ===", 
                    jobExecution.getStatus(), duration);
            }
        };
    }

    /**
     * Chunk 기반 스트리밍 Step
     * - Reader: 1개씩 생성 (메모리 O(1))
     * - Writer: 5000개씩 Batch Insert (속도 최적화)
     */
    @Bean
    @StepScope
    public Step dataGenerationStep(
            @Value("#{jobParameters['tableName']}") String tableName,
            @Value("#{jobParameters['schema']}") String schemaJson,
            @Value("#{jobParameters['rowCount']}") int rowCount,
            @Value("#{jobParameters['seed']}") long seed) {
        
        TableMetadata table = parseSchema(schemaJson);
        List<String> columnNames = table.getColumns().stream()
            .map(ColumnMetadata::getName)
            .collect(Collectors.toList());
        
        return stepBuilderFactory.get("dataGenerationStep")
            .<Map<String, Object>, Map<String, Object>>chunk(CHUNK_SIZE)
            .reader(new DataGeneratingItemReader(generatorService, table, rowCount, seed))
            .processor(new PassThroughItemProcessor<>())
            .writer(items -> {
                jdbcWriter.setTableInfo(tableName, columnNames);
                jdbcWriter.write(items);
            })
            .faultTolerant()
            .skipLimit(100)
            .skip(DataAccessException.class)
            .listener(chunkListener(rowCount))
            .build();
    }

    private ChunkListener chunkListener(int totalRows) {
        return new ChunkListener() {
            @Override
            public void afterChunk(ChunkContext context) {
                long readCount = context.getStepContext()
                    .getStepExecution().getReadCount();
                int percent = (int) (readCount * 100 / totalRows);
                log.info("Progress: {} / {} rows ({}%)", readCount, totalRows, percent);
            }
            
            @Override public void beforeChunk(ChunkContext context) {}
            @Override public void afterChunkError(ChunkContext context) {}
        };
    }
}
```

### 4.5 Frontend - EventSource 기반 실시간 수신

#### streamingApi.js

```javascript
/**
 * SSE 기반 실시간 데이터 수신
 */
export const streamGenerate = (tableName, rowCount, onProgress, onData, onComplete, onError) => {
    const eventSource = new EventSource(
        `${API_URL}/api/orchestrator/stream?tableName=${tableName}&rowCount=${rowCount}`
    );
    
    eventSource.addEventListener('data', (event) => {
        const chunk = JSON.parse(event.data);
        onData(chunk.rows);
        onProgress(chunk.progress, chunk.total);
    });
    
    eventSource.addEventListener('complete', () => {
        eventSource.close();
        onComplete();
    });
    
    eventSource.onerror = (error) => {
        console.error('SSE Error:', error);
        eventSource.close();
        onError(error);
    };
    
    // Cleanup function
    return () => eventSource.close();
};

/**
 * 스트리밍 파일 다운로드
 */
export const downloadCsv = (tableName, rowCount) => {
    // 브라우저가 스트리밍으로 파일 저장
    window.location.href = 
        `${API_URL}/api/orchestrator/download/csv?tableName=${tableName}&rowCount=${rowCount}`;
};
```

#### StreamingPreview.jsx

```jsx
import React, { useState, useEffect } from 'react';
import { streamGenerate, downloadCsv } from '../api/streamingApi';

const StreamingPreview = ({ tableName, rowCount }) => {
    const [data, setData] = useState([]);
    const [progress, setProgress] = useState(0);
    const [total, setTotal] = useState(0);
    const [isComplete, setIsComplete] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleStartGenerate = () => {
        setIsLoading(true);
        setData([]);
        setProgress(0);
        
        const cleanup = streamGenerate(
            tableName,
            rowCount,
            (current, total) => {
                setProgress(current);
                setTotal(total);
            },
            (rows) => {
                // 미리보기는 최대 100개만 표시
                setData(prev => prev.length < 100 ? [...prev, ...rows.slice(0, 100 - prev.length)] : prev);
            },
            () => {
                setIsComplete(true);
                setIsLoading(false);
            },
            (error) => {
                console.error(error);
                setIsLoading(false);
            }
        );
        
        return cleanup;
    };

    const progressPercent = total > 0 ? Math.round((progress / total) * 100) : 0;

    return (
        <div className="streaming-preview">
            <div className="controls">
                <button onClick={handleStartGenerate} disabled={isLoading}>
                    {isLoading ? '생성 중...' : '스트리밍 생성 시작'}
                </button>
                <button onClick={() => downloadCsv(tableName, rowCount)}>
                    CSV 다운로드
                </button>
            </div>

            {isLoading && (
                <div className="progress-bar">
                    <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
                    <span>{progress.toLocaleString()} / {total.toLocaleString()} ({progressPercent}%)</span>
                </div>
            )}

            <div className="preview-table">
                <table>
                    <thead>
                        <tr>
                            {data[0] && Object.keys(data[0]).map(key => (
                                <th key={key}>{key}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {data.map((row, idx) => (
                            <tr key={idx}>
                                {Object.values(row).map((val, i) => (
                                    <td key={i}>{String(val)}</td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {isComplete && <p className="complete-message">✅ 생성 완료!</p>}
        </div>
    );
};

export default StreamingPreview;
```

---

## 5. 성능 비교

### 5.1 메모리 사용량

| 시나리오 | 기존 방식 | True Streaming |
|:---|:---:|:---:|
| 10만 건 | ~200MB | ~50MB |
| 100만 건 | ~2GB (OOM 위험) | ~50MB |
| 1000만 건 | 불가능 | ~50MB |

### 5.2 응답 속도

| 지표 | 기존 방식 | True Streaming |
|:---|:---:|:---:|
| 첫 데이터 수신 | 완료 후 (수 분) | 즉시 (~100ms) |
| 진행률 표시 | 불가능 | 실시간 |
| 사용자 경험 | 멈춤 상태 | 능동적 피드백 |

### 5.3 DB Insert 성능

| 방식 | 100만 건 소요 시간 | 메모리 |
|:---|:---:|:---:|
| 단일 INSERT | ~30분 | 2GB+ |
| Batch INSERT (1000개) | ~5분 | 100MB |
| Batch INSERT (5000개) | ~3분 | 100MB |

---

## 6. 구현 우선순위

| 우선순위 | 구현 항목 | 효과 | 난이도 |
|:---:|:---|:---|:---:|
| 1️⃣ | Generator Stream 기반 생성 | 메모리 90% 감소 | 낮음 |
| 2️⃣ | StreamingResponseBody CSV 다운로드 | 즉시 다운로드 시작 | 낮음 |
| 3️⃣ | Spring Batch ItemReader | 대용량 DB Insert | 중간 |
| 4️⃣ | SSE 실시간 진행률 | UX 대폭 개선 | 중간 |
| 5️⃣ | Frontend EventSource | 실시간 미리보기 | 낮음 |

---

## 7. 추가 최적화 옵션

### 7.1 병렬 처리 (Partitioning)

```java
@Bean
public Step masterStep() {
    return stepBuilderFactory.get("masterStep")
        .partitioner("slaveStep", new RangePartitioner(totalRows, 10))
        .step(slaveStep())
        .gridSize(10)  // 10개 병렬 처리
        .taskExecutor(new SimpleAsyncTaskExecutor())
        .build();
}
```

### 7.2 Virtual Scrolling (Frontend)

```jsx
import { FixedSizeList as List } from 'react-window';

<List height={600} itemCount={1000000} itemSize={35} width="100%">
    {({ index, style }) => (
        <Row style={style} data={data[index]} />
    )}
</List>
```

### 7.3 R2DBC Non-blocking Insert

```java
@Service
public class R2dbcInsertService {
    private final R2dbcEntityTemplate template;
    
    public Mono<Long> insertStream(Flux<Map<String, Object>> dataStream) {
        return dataStream
            .buffer(5000)
            .concatMap(this::insertBatch)
            .reduce(0L, Long::sum);
    }
}
```

---

## 8. 참고 자료

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Spring Batch Scaling and Parallel Processing](https://docs.spring.io/spring-batch/docs/current/reference/html/scalability.html)
- [Server-Sent Events (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [JDBC Batch Operations](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#jdbc-advanced-jdbc)
