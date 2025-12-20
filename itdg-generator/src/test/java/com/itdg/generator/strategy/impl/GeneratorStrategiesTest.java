package com.itdg.generator.strategy.impl;

import com.itdg.common.dto.metadata.ColumnMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데이터 생성 전략 단위 테스트
 */
class GeneratorStrategiesTest {

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random(12345L);
    }

    private ColumnMetadata createColumn(String name, String dataType) {
        return createColumn(name, dataType, null);
    }

    private ColumnMetadata createColumn(String name, String dataType, Integer length) {
        return ColumnMetadata.builder()
                .name(name)
                .dataType(dataType)
                .length(length)
                .build();
    }

    // ============================================
    // StringGeneratorStrategy 테스트
    // ============================================
    @Nested
    @DisplayName("StringGeneratorStrategy 테스트")
    class StringGeneratorStrategyTest {
        private StringGeneratorStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new StringGeneratorStrategy();
        }

        @Test
        @DisplayName("문자열 타입을 지원해야 함")
        void supports_stringTypes_returnsTrue() {
            assertThat(strategy.supports(createColumn("name", "VARCHAR"))).isTrue();
            assertThat(strategy.supports(createColumn("description", "TEXT"))).isTrue();
            assertThat(strategy.supports(createColumn("code", "CHAR"))).isTrue();
            assertThat(strategy.supports(createColumn("value", "STRING"))).isTrue();
        }

        @Test
        @DisplayName("숫자 타입은 지원하지 않아야 함")
        void supports_numericTypes_returnsFalse() {
            assertThat(strategy.supports(createColumn("count", "INTEGER"))).isFalse();
            assertThat(strategy.supports(createColumn("amount", "DECIMAL"))).isFalse();
        }

        @Test
        @DisplayName("null 타입은 지원하지 않아야 함")
        void supports_nullType_returnsFalse() {
            assertThat(strategy.supports(createColumn("test", null))).isFalse();
        }

        @Test
        @DisplayName("지정된 길이만큼 문자열 생성")
        void generate_withLength_returnsStringOfLength() {
            ColumnMetadata column = createColumn("code", "VARCHAR", 10);
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String value = (String) result;
            assertThat(value.length()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("길이 미지정시 기본값 사용")
        void generate_withoutLength_usesDefault() {
            ColumnMetadata column = createColumn("name", "VARCHAR");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            assertThat(((String) result).length()).isLessThanOrEqualTo(36); // UUID length
        }

        @Test
        @DisplayName("너무 긴 길이는 100으로 제한")
        void generate_withLongLength_limitsTo100() {
            ColumnMetadata column = createColumn("content", "TEXT", 500);
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            // UUID는 36자이므로 100보다 작음
            assertThat(((String) result).length()).isLessThanOrEqualTo(100);
        }
    }

    // ============================================
    // NumericGeneratorStrategy 테스트
    // ============================================
    @Nested
    @DisplayName("NumericGeneratorStrategy 테스트")
    class NumericGeneratorStrategyTest {
        private NumericGeneratorStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new NumericGeneratorStrategy();
        }

        @Test
        @DisplayName("숫자 타입을 지원해야 함")
        void supports_numericTypes_returnsTrue() {
            assertThat(strategy.supports(createColumn("count", "INTEGER"))).isTrue();
            assertThat(strategy.supports(createColumn("amount", "BIGINT"))).isTrue();
            assertThat(strategy.supports(createColumn("price", "DECIMAL"))).isTrue();
            assertThat(strategy.supports(createColumn("rate", "DOUBLE"))).isTrue();
            assertThat(strategy.supports(createColumn("value", "FLOAT"))).isTrue();
            assertThat(strategy.supports(createColumn("num", "NUMERIC"))).isTrue();
            assertThat(strategy.supports(createColumn("total", "NUMBER"))).isTrue();
            assertThat(strategy.supports(createColumn("score", "REAL"))).isTrue();
        }

        @Test
        @DisplayName("문자열 타입은 지원하지 않아야 함")
        void supports_stringTypes_returnsFalse() {
            assertThat(strategy.supports(createColumn("name", "VARCHAR"))).isFalse();
            assertThat(strategy.supports(createColumn("desc", "TEXT"))).isFalse();
        }

        @Test
        @DisplayName("INTEGER 타입은 Integer 반환")
        void generate_integerType_returnsInteger() {
            ColumnMetadata column = createColumn("count", "INTEGER");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(Integer.class);
            assertThat((Integer) result).isBetween(0, 10000);
        }

        @Test
        @DisplayName("SERIAL 타입은 Integer 반환")
        void generate_serialType_returnsInteger() {
            ColumnMetadata column = createColumn("id", "SERIAL");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("DOUBLE 타입은 Double 반환")
        void generate_doubleType_returnsDouble() {
            ColumnMetadata column = createColumn("rate", "DOUBLE");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(Double.class);
            assertThat((Double) result).isBetween(0.0, 1000.0);
        }

        @Test
        @DisplayName("FLOAT 타입은 Double 반환")
        void generate_floatType_returnsDouble() {
            ColumnMetadata column = createColumn("value", "FLOAT");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(Double.class);
        }

        @Test
        @DisplayName("DECIMAL 타입은 BigDecimal 반환")
        void generate_decimalType_returnsBigDecimal() {
            ColumnMetadata column = createColumn("price", "DECIMAL");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(BigDecimal.class);
        }

        @Test
        @DisplayName("NUMERIC 타입은 BigDecimal 반환")
        void generate_numericType_returnsBigDecimal() {
            ColumnMetadata column = createColumn("amount", "NUMERIC");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(BigDecimal.class);
        }
    }

    // ============================================
    // DateTimeGeneratorStrategy 테스트
    // ============================================
    @Nested
    @DisplayName("DateTimeGeneratorStrategy 테스트")
    class DateTimeGeneratorStrategyTest {
        private DateTimeGeneratorStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new DateTimeGeneratorStrategy();
        }

        @Test
        @DisplayName("날짜/시간 타입을 지원해야 함")
        void supports_dateTimeTypes_returnsTrue() {
            assertThat(strategy.supports(createColumn("created_at", "DATE"))).isTrue();
            assertThat(strategy.supports(createColumn("updated_at", "DATETIME"))).isTrue();
            assertThat(strategy.supports(createColumn("logged_at", "TIMESTAMP"))).isTrue();
            assertThat(strategy.supports(createColumn("start_time", "TIME"))).isTrue();
        }

        @Test
        @DisplayName("문자열 타입은 지원하지 않아야 함")
        void supports_stringTypes_returnsFalse() {
            assertThat(strategy.supports(createColumn("name", "VARCHAR"))).isFalse();
        }

        @Test
        @DisplayName("DATE 타입에 대해 날짜 생성")
        void generate_dateType_returnsDate() {
            ColumnMetadata column = createColumn("created_at", "DATE");
            Object result = strategy.generate(column, random);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TIMESTAMP 타입에 대해 날짜시간 생성")
        void generate_timestampType_returnsDateTime() {
            ColumnMetadata column = createColumn("created_at", "TIMESTAMP");
            Object result = strategy.generate(column, random);

            assertThat(result).isNotNull();
        }
    }

    // ============================================
    // BooleanGeneratorStrategy 테스트
    // ============================================
    @Nested
    @DisplayName("BooleanGeneratorStrategy 테스트")
    class BooleanGeneratorStrategyTest {
        private BooleanGeneratorStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new BooleanGeneratorStrategy();
        }

        @Test
        @DisplayName("Boolean 타입을 지원해야 함")
        void supports_booleanTypes_returnsTrue() {
            assertThat(strategy.supports(createColumn("is_active", "BOOLEAN"))).isTrue();
            assertThat(strategy.supports(createColumn("flag", "BOOL"))).isTrue();
            assertThat(strategy.supports(createColumn("enabled", "BIT"))).isTrue();
        }

        @Test
        @DisplayName("숫자 타입은 지원하지 않아야 함")
        void supports_numericTypes_returnsFalse() {
            assertThat(strategy.supports(createColumn("count", "INTEGER"))).isFalse();
        }

        @Test
        @DisplayName("Boolean 값을 생성해야 함")
        void generate_returnsBoolean() {
            ColumnMetadata column = createColumn("is_active", "BOOLEAN");
            Object result = strategy.generate(column, random);

            assertThat(result).isInstanceOf(Boolean.class);
        }

        @Test
        @DisplayName("여러 번 생성시 true/false 모두 나와야 함")
        void generate_multipleRuns_producesBothValues() {
            ColumnMetadata column = createColumn("is_active", "BOOLEAN");

            boolean hasTrue = false;
            boolean hasFalse = false;

            for (int i = 0; i < 100; i++) {
                Boolean result = (Boolean) strategy.generate(column, random);
                if (result)
                    hasTrue = true;
                else
                    hasFalse = true;

                if (hasTrue && hasFalse)
                    break;
            }

            assertThat(hasTrue).isTrue();
            assertThat(hasFalse).isTrue();
        }
    }
}
