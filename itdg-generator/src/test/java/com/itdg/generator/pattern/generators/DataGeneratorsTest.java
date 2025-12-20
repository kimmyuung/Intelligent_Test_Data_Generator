package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데이터 생성기 단위 테스트
 */
class DataGeneratorsTest {

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random(12345L); // 고정 시드로 재현 가능한 테스트
    }

    private ColumnMetadata createColumn(String name, String dataType) {
        return ColumnMetadata.builder()
                .name(name)
                .dataType(dataType)
                .build();
    }

    // ============================================
    // NameGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("NameGenerator 테스트")
    class NameGeneratorTest {
        private NameGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new NameGenerator();
        }

        @Test
        @DisplayName("name 컬럼을 지원해야 함")
        void supports_nameColumn_returnsTrue() {
            assertThat(generator.supports(createColumn("name", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("user_name", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("username", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("full_nm", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("id가 포함된 컬럼은 지원하지 않아야 함")
        void supports_idColumn_returnsFalse() {
            assertThat(generator.supports(createColumn("name_id", "VARCHAR"))).isFalse();
        }

        @Test
        @DisplayName("null 컬럼명은 지원하지 않아야 함")
        void supports_nullName_returnsFalse() {
            assertThat(generator.supports(createColumn(null, "VARCHAR"))).isFalse();
        }

        @Test
        @DisplayName("한글 이름을 생성해야 함")
        void generate_stringType_returnsKoreanName() {
            ColumnMetadata column = createColumn("name", "VARCHAR");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String name = (String) result;
            assertThat(name).isNotEmpty();
            assertThat(name.length()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("문자열이 아닌 타입은 에러 반환")
        void generate_nonStringType_returnsError() {
            ColumnMetadata column = createColumn("name", "INTEGER");
            Object result = generator.generate(column, random);

            assertThat(result).isEqualTo("ERROR_NOT_STRING");
        }
    }

    // ============================================
    // EmailGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("EmailGenerator 테스트")
    class EmailGeneratorTest {
        private EmailGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new EmailGenerator();
        }

        @Test
        @DisplayName("email 컬럼을 지원해야 함")
        void supports_emailColumn_returnsTrue() {
            assertThat(generator.supports(createColumn("email", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("user_email", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("mail", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("이메일 형식을 생성해야 함")
        void generate_returnsValidEmailFormat() {
            ColumnMetadata column = createColumn("email", "VARCHAR");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String email = (String) result;
            assertThat(email).contains("@example.com");
            assertThat(email).startsWith("user");
        }
    }

    // ============================================
    // PhoneGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("PhoneGenerator 테스트")
    class PhoneGeneratorTest {
        private PhoneGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new PhoneGenerator();
        }

        @Test
        @DisplayName("phone 관련 컬럼을 지원해야 함")
        void supports_phoneColumn_returnsTrue() {
            assertThat(generator.supports(createColumn("phone", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("telephone", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("mobile", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("contact", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("한국 전화번호 형식을 생성해야 함")
        void generate_returnsKoreanPhoneFormat() {
            ColumnMetadata column = createColumn("phone", "VARCHAR");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String phone = (String) result;
            assertThat(phone).matches("010-\\d{4}-\\d{4}");
        }
    }

    // ============================================
    // AddressGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("AddressGenerator 테스트")
    class AddressGeneratorTest {
        private AddressGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new AddressGenerator();
        }

        @Test
        @DisplayName("address 관련 컬럼을 지원해야 함")
        void supports_addressColumn_returnsTrue() {
            assertThat(generator.supports(createColumn("address", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("addr", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("location", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("place", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("한국 주소 형식을 생성해야 함")
        void generate_returnsKoreanAddressFormat() {
            ColumnMetadata column = createColumn("address", "VARCHAR");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String address = (String) result;
            assertThat(address).isNotEmpty();
            // 주소에 공백 포함 확인
            assertThat(address.split(" ").length).isGreaterThanOrEqualTo(4);
        }
    }

    // ============================================
    // DateGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("DateGenerator 테스트")
    class DateGeneratorTest {
        private DateGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new DateGenerator();
        }

        @Test
        @DisplayName("DATE/TIME 타입을 지원해야 함")
        void supports_dateTimeTypes_returnsTrue() {
            assertThat(generator.supports(createColumn("created_at", "DATE"))).isTrue();
            assertThat(generator.supports(createColumn("updated_at", "TIMESTAMP"))).isTrue();
            assertThat(generator.supports(createColumn("start_time", "TIME"))).isTrue();
            assertThat(generator.supports(createColumn("created", "DATETIME"))).isTrue();
        }

        @Test
        @DisplayName("DATE 타입은 날짜 형식 반환")
        void generate_dateType_returnsDateFormat() {
            ColumnMetadata column = createColumn("created_at", "DATE");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String date = (String) result;
            assertThat(date).matches("\\d{4}-\\d{2}-\\d{2}");
        }

        @Test
        @DisplayName("TIMESTAMP 타입은 날짜시간 형식 반환")
        void generate_timestampType_returnsDateTimeFormat() {
            ColumnMetadata column = createColumn("created_at", "TIMESTAMP");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String datetime = (String) result;
            assertThat(datetime).contains("T"); // ISO 형식
        }

        @Test
        @DisplayName("TIME 타입은 시간 형식 반환")
        void generate_timeType_returnsTimeFormat() {
            ColumnMetadata column = createColumn("start_time", "TIME");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String time = (String) result;
            assertThat(time).matches("\\d{2}:\\d{2}:\\d{2}");
        }
    }

    // ============================================
    // NumberGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("NumberGenerator 테스트")
    class NumberGeneratorTest {
        private NumberGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new NumberGenerator();
        }

        @Test
        @DisplayName("숫자 타입을 지원해야 함")
        void supports_numericTypes_returnsTrue() {
            assertThat(generator.supports(createColumn("count", "INTEGER"))).isTrue();
            assertThat(generator.supports(createColumn("amount", "BIGINT"))).isTrue();
            assertThat(generator.supports(createColumn("price", "DECIMAL"))).isTrue();
            assertThat(generator.supports(createColumn("rate", "DOUBLE"))).isTrue();
            assertThat(generator.supports(createColumn("quantity", "FLOAT"))).isTrue();
        }

        @Test
        @DisplayName("INTEGER 타입은 정수 반환")
        void generate_integerType_returnsInteger() {
            ColumnMetadata column = createColumn("count", "INTEGER");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(Integer.class);
            assertThat((Integer) result).isGreaterThanOrEqualTo(0);
            assertThat((Integer) result).isLessThan(10000);
        }

        @Test
        @DisplayName("BIGINT 타입은 Long 반환")
        void generate_bigintType_returnsLong() {
            ColumnMetadata column = createColumn("id", "BIGINT");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("DECIMAL 타입은 소수점 문자열 반환")
        void generate_decimalType_returnsFormattedString() {
            ColumnMetadata column = createColumn("price", "DECIMAL");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String value = (String) result;
            assertThat(value).matches("\\d+\\.\\d{2}");
        }
    }

    // ============================================
    // BooleanGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("BooleanGenerator 테스트")
    class BooleanGeneratorTest {
        private BooleanGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new BooleanGenerator();
        }

        @Test
        @DisplayName("BOOLEAN 타입을 지원해야 함")
        void supports_booleanType_returnsTrue() {
            assertThat(generator.supports(createColumn("is_active", "BOOLEAN"))).isTrue();
            assertThat(generator.supports(createColumn("flag", "BOOL"))).isTrue();
        }

        @Test
        @DisplayName("boolean 값을 생성해야 함")
        void generate_returnsBoolean() {
            ColumnMetadata column = createColumn("is_active", "BOOLEAN");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(Boolean.class);
        }
    }

    // ============================================
    // UrlGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("UrlGenerator 테스트")
    class UrlGeneratorTest {
        private UrlGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new UrlGenerator();
        }

        @Test
        @DisplayName("url 관련 컬럼을 지원해야 함")
        void supports_urlColumn_returnsTrue() {
            assertThat(generator.supports(createColumn("url", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("website", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("homepage", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("link", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("URL 형식을 생성해야 함")
        void generate_returnsValidUrlFormat() {
            ColumnMetadata column = createColumn("url", "VARCHAR");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String url = (String) result;
            assertThat(url).startsWith("https://");
        }
    }

    // ============================================
    // UuidGenerator 테스트
    // ============================================
    @Nested
    @DisplayName("UuidGenerator 테스트")
    class UuidGeneratorTest {
        private UuidGenerator generator;

        @BeforeEach
        void setUp() {
            generator = new UuidGenerator();
        }

        @Test
        @DisplayName("UUID 타입을 지원해야 함")
        void supports_uuidType_returnsTrue() {
            assertThat(generator.supports(createColumn("id", "UUID"))).isTrue();
            assertThat(generator.supports(createColumn("external_uuid", "VARCHAR"))).isTrue();
            assertThat(generator.supports(createColumn("guid", "VARCHAR"))).isTrue();
        }

        @Test
        @DisplayName("UUID 형식을 생성해야 함")
        void generate_returnsValidUuidFormat() {
            ColumnMetadata column = createColumn("id", "UUID");
            Object result = generator.generate(column, random);

            assertThat(result).isInstanceOf(String.class);
            String uuid = (String) result;
            // UUID 형식: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            assertThat(uuid).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }
}
