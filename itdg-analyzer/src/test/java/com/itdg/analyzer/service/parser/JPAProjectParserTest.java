package com.itdg.analyzer.service.parser;

import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPAProjectParser 단위 테스트
 */
class JPAProjectParserTest {

    private JPAProjectParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new JPAProjectParser();
    }

    @Test
    @DisplayName("build.gradle이 있는 디렉토리를 지원해야 함")
    void supports_withBuildGradle_returnsTrue() throws IOException {
        Files.createFile(tempDir.resolve("build.gradle"));

        assertThat(parser.supports(tempDir.toFile())).isTrue();
    }

    @Test
    @DisplayName("pom.xml이 있는 디렉토리를 지원해야 함")
    void supports_withPomXml_returnsTrue() throws IOException {
        Files.createFile(tempDir.resolve("pom.xml"));

        assertThat(parser.supports(tempDir.toFile())).isTrue();
    }

    @Test
    @DisplayName("빌드 파일이 없는 디렉토리는 지원하지 않음")
    void supports_withoutBuildFile_returnsFalse() {
        assertThat(parser.supports(tempDir.toFile())).isFalse();
    }

    @Test
    @DisplayName("프로젝트 정보를 올바르게 감지해야 함")
    void detectInfo_returnsCorrectProjectInfo() throws IOException {
        Files.createFile(tempDir.resolve("build.gradle"));

        ProjectInfo info = parser.detectInfo(tempDir.toFile());

        assertThat(info).isNotNull();
        assertThat(info.getLanguage()).isEqualTo("Java");
        assertThat(info.getFramework()).isEqualTo("JPA/Hibernate");
    }

    @Test
    @DisplayName("JPA Entity를 파싱해야 함")
    void parse_withJpaEntity_returnsTableMetadata() throws IOException {
        // Given: JPA Entity 파일 생성
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String entityCode = """
                package com.example;

                import jakarta.persistence.*;

                @Entity
                @Table(name = "users")
                public class User {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;

                    @Column(name = "username", nullable = false, length = 50)
                    private String username;

                    @Column(name = "email")
                    private String email;

                    @Column(nullable = false)
                    private Boolean isActive;
                }
                """;

        Files.writeString(srcDir.resolve("User.java"), entityCode);
        Files.createFile(tempDir.resolve("build.gradle"));

        // When
        List<TableMetadata> tables = parser.parse(tempDir.toFile());

        // Then
        assertThat(tables).isNotEmpty();
        assertThat(tables).hasSize(1);

        TableMetadata userTable = tables.get(0);
        assertThat(userTable.getTableName()).isEqualTo("users");
        assertThat(userTable.getColumns()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("@Table 어노테이션이 없으면 클래스명 사용")
    void parse_withoutTableAnnotation_usesClassName() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String entityCode = """
                package com.example;

                import jakarta.persistence.*;

                @Entity
                public class Product {
                    @Id
                    private Long id;
                    private String name;
                }
                """;

        Files.writeString(srcDir.resolve("Product.java"), entityCode);
        Files.createFile(tempDir.resolve("build.gradle"));

        List<TableMetadata> tables = parser.parse(tempDir.toFile());

        assertThat(tables).isNotEmpty();
        // 클래스명을 소문자로 변환하여 테이블명으로 사용
        assertThat(tables.get(0).getTableName().toLowerCase()).contains("product");
    }

    @Test
    @DisplayName("Entity가 없는 프로젝트는 빈 리스트 반환")
    void parse_withoutEntities_returnsEmptyList() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String normalClass = """
                package com.example;

                public class UtilityClass {
                    public static String helper() {
                        return "helper";
                    }
                }
                """;

        Files.writeString(srcDir.resolve("UtilityClass.java"), normalClass);
        Files.createFile(tempDir.resolve("build.gradle"));

        List<TableMetadata> tables = parser.parse(tempDir.toFile());

        assertThat(tables).isEmpty();
    }

    @Test
    @DisplayName("여러 Entity를 파싱해야 함")
    void parse_withMultipleEntities_returnsAllTables() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String userEntity = """
                package com.example;
                import jakarta.persistence.*;

                @Entity
                public class User {
                    @Id
                    private Long id;
                    private String name;
                }
                """;

        String orderEntity = """
                package com.example;
                import jakarta.persistence.*;

                @Entity
                @Table(name = "orders")
                public class Order {
                    @Id
                    private Long id;
                    private String description;
                }
                """;

        Files.writeString(srcDir.resolve("User.java"), userEntity);
        Files.writeString(srcDir.resolve("Order.java"), orderEntity);
        Files.createFile(tempDir.resolve("build.gradle"));

        List<TableMetadata> tables = parser.parse(tempDir.toFile());

        assertThat(tables).hasSize(2);
    }
}
