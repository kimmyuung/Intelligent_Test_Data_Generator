package com.itdg.generator.service;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import com.itdg.common.dto.request.GenerateDataRequest;
import com.itdg.common.dto.response.GenerateDataResponse;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DataGeneratorService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataGeneratorService 테스트")
class DataGeneratorServiceTest {

    @Mock
    private DataGeneratorStrategy mockStrategy;

    private DataGeneratorService dataGeneratorService;

    @BeforeEach
    void setUp() {
        // Strategy 목록 설정
        List<DataGeneratorStrategy> strategies = List.of(mockStrategy);
        dataGeneratorService = new DataGeneratorService(strategies);
    }

    // =========================================
    // Helper Methods
    // =========================================

    private TableMetadata createSimpleTable(String tableName, List<ColumnMetadata> columns) {
        return TableMetadata.builder()
                .tableName(tableName)
                .columns(columns)
                .primaryKeys(new ArrayList<>())
                .build();
    }

    private ColumnMetadata createColumn(String name, String dataType, boolean isPrimaryKey, boolean isNullable) {
        return ColumnMetadata.builder()
                .name(name)
                .dataType(dataType)
                .isPrimaryKey(isPrimaryKey)
                .isNullable(isNullable)
                .isAutoIncrement(isPrimaryKey)
                .build();
    }

    private GenerateDataRequest createRequest(SchemaMetadata schema, int rowCount, Long seed) {
        return GenerateDataRequest.builder()
                .schema(schema)
                .rowCount(rowCount)
                .seed(seed)
                .build();
    }

    // =========================================
    // generateData() 테스트
    // =========================================

    @Nested
    @DisplayName("generateData() 메서드")
    class GenerateDataTests {

        @Test
        @DisplayName("정상적인 요청으로 데이터를 성공적으로 생성한다")
        void generateData_withValidRequest_returnsSuccess() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false),
                    createColumn("name", "VARCHAR", false, false),
                    createColumn("email", "VARCHAR", false, true));
            TableMetadata table = createSimpleTable("users", columns);
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 10, 12345L);

            when(mockStrategy.supports(any(ColumnMetadata.class))).thenReturn(true);
            when(mockStrategy.generate(any(ColumnMetadata.class), any(Random.class)))
                    .thenReturn("test_value");

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGeneratedData()).containsKey("users");
            assertThat(response.getGeneratedData().get("users")).hasSize(10);
            assertThat(response.getStatistics().get("users")).isEqualTo(10);
        }

        @Test
        @DisplayName("스키마가 null이면 실패 응답을 반환한다")
        void generateData_withNullSchema_returnsFailure() {
            // Given
            GenerateDataRequest request = createRequest(null, 10, 12345L);

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("null");
        }

        @Test
        @DisplayName("테이블 목록이 null이면 실패 응답을 반환한다")
        void generateData_withNullTables_returnsFailure() {
            // Given
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(null)
                    .build();
            GenerateDataRequest request = createRequest(schema, 10, 12345L);

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("seed가 null이면 현재 시간을 사용한다")
        void generateData_withNullSeed_usesCurrentTime() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false));
            TableMetadata table = createSimpleTable("test_table", columns);
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 5, null);

            when(mockStrategy.supports(any())).thenReturn(true);
            when(mockStrategy.generate(any(), any())).thenReturn("value");

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getSeed()).isPositive();
        }

        @Test
        @DisplayName("rowCount가 null이면 기본값 100을 사용한다")
        void generateData_withNullRowCount_usesDefault100() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false));
            TableMetadata table = createSimpleTable("test_table", columns);
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = GenerateDataRequest.builder()
                    .schema(schema)
                    .rowCount(null)
                    .seed(12345L)
                    .build();

            when(mockStrategy.supports(any())).thenReturn(true);
            when(mockStrategy.generate(any(), any())).thenReturn("value");

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getGeneratedData().get("test_table")).hasSize(100);
        }
    }

    // =========================================
    // generateDataStream() 테스트
    // =========================================

    @Nested
    @DisplayName("generateDataStream() 메서드")
    class GenerateDataStreamTests {

        @Test
        @DisplayName("스트림으로 지정된 개수만큼 데이터를 생성한다")
        void generateDataStream_returnsCorrectNumberOfRows() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false),
                    createColumn("value", "VARCHAR", false, false));
            TableMetadata table = createSimpleTable("stream_test", columns);

            when(mockStrategy.supports(any())).thenReturn(true);
            when(mockStrategy.generate(any(), any())).thenReturn("stream_value");

            // When
            List<Map<String, Object>> result = dataGeneratorService
                    .generateDataStream(table, 50, 12345L)
                    .collect(Collectors.toList());

            // Then
            assertThat(result).hasSize(50);
        }

        @Test
        @DisplayName("동일한 seed로 동일한 결과를 생성한다")
        void generateDataStream_withSameSeed_producesSameResults() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false));
            TableMetadata table = createSimpleTable("seed_test", columns);
            long seed = 12345L;

            // When
            List<Map<String, Object>> result1 = dataGeneratorService
                    .generateDataStream(table, 10, seed)
                    .collect(Collectors.toList());

            List<Map<String, Object>> result2 = dataGeneratorService
                    .generateDataStream(table, 10, seed)
                    .collect(Collectors.toList());

            // Then
            assertThat(result1).hasSize(10);
            assertThat(result2).hasSize(10);
            // PK는 동일해야 함
            for (int i = 0; i < 10; i++) {
                assertThat(result1.get(i).get("id")).isEqualTo(result2.get(i).get("id"));
            }
        }
    }

    // =========================================
    // Primary Key & Foreign Key 테스트
    // =========================================

    @Nested
    @DisplayName("Primary Key 생성 테스트")
    class PrimaryKeyTests {

        @Test
        @DisplayName("INTEGER PK는 순차적으로 증가한다")
        void generateData_withIntegerPK_generatesSequentialIds() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false));
            TableMetadata table = createSimpleTable("pk_test", columns);
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 5, 12345L);

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            List<Map<String, Object>> rows = response.getGeneratedData().get("pk_test");

            for (int i = 0; i < 5; i++) {
                assertThat(rows.get(i).get("id")).isEqualTo((long) (i + 1));
            }
        }

        @Test
        @DisplayName("VARCHAR PK는 UUID를 생성한다")
        void generateData_withVarcharPK_generatesUUIDs() {
            // Given
            ColumnMetadata pkColumn = ColumnMetadata.builder()
                    .name("id")
                    .dataType("VARCHAR")
                    .isPrimaryKey(true)
                    .isNullable(false)
                    .isAutoIncrement(false)
                    .build();
            TableMetadata table = createSimpleTable("uuid_test", List.of(pkColumn));
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 3, 12345L);

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            List<Map<String, Object>> rows = response.getGeneratedData().get("uuid_test");

            for (Map<String, Object> row : rows) {
                String id = (String) row.get("id");
                assertThat(id).matches("[a-f0-9-]{36}"); // UUID 형식
            }
        }
    }

    @Nested
    @DisplayName("Foreign Key 생성 테스트")
    class ForeignKeyTests {

        @Test
        @DisplayName("FK 컬럼은 1-10 범위의 정수를 생성한다")
        void generateData_withForeignKey_generatesValidReferences() {
            // Given
            ColumnMetadata fkColumn = ColumnMetadata.builder()
                    .name("user_id")
                    .dataType("INTEGER")
                    .isPrimaryKey(false)
                    .isForeignKey(true)
                    .isNullable(false)
                    .build();
            TableMetadata table = createSimpleTable("fk_test", List.of(fkColumn));
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 20, 12345L);

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            List<Map<String, Object>> rows = response.getGeneratedData().get("fk_test");

            for (Map<String, Object> row : rows) {
                Integer userId = (Integer) row.get("user_id");
                assertThat(userId).isBetween(1, 10);
            }
        }
    }

    // =========================================
    // Strategy 테스트
    // =========================================

    @Nested
    @DisplayName("Strategy 패턴 테스트")
    class StrategyTests {

        @Test
        @DisplayName("지원하지 않는 컬럼에는 N/A를 반환한다")
        void generateData_withUnsupportedColumn_returnsNA() {
            // Given
            List<ColumnMetadata> columns = List.of(
                    createColumn("id", "INTEGER", true, false),
                    createColumn("unknown", "BLOB", false, true));
            TableMetadata table = createSimpleTable("fallback_test", columns);
            SchemaMetadata schema = SchemaMetadata.builder()
                    .tables(List.of(table))
                    .build();
            GenerateDataRequest request = createRequest(schema, 1, 12345L);

            // Strategy가 미지원 컬럼에 대해 false 반환
            when(mockStrategy.supports(any(ColumnMetadata.class))).thenAnswer(invocation -> {
                ColumnMetadata col = invocation.getArgument(0);
                return !"BLOB".equals(col.getDataType());
            });
            when(mockStrategy.generate(any(), any())).thenReturn("generated");

            // When
            GenerateDataResponse response = dataGeneratorService.generateData(request);

            // Then
            assertThat(response.isSuccess()).isTrue();
            Map<String, Object> row = response.getGeneratedData().get("fallback_test").get(0);
            assertThat(row.get("unknown")).isEqualTo("N/A");
        }
    }
}
