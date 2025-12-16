package com.itdg.analyzer.service.parser;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.TableMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
public class KotlinProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith(".kt"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Kotlin")
                .framework("JPA/Spring")
                .analyzedFiles(0)
                .totalFiles(countKtFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countKtFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".kt")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".kt"))
                    .forEach(path -> parseKtFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Kotlin project", e);
        }
        return tables;
    }

    private void parseKtFile(Path path, List<TableMetadata> tables) {
        try {
            // Regex for checking @Entity
            String content = Files.readString(path);
            if (!content.contains("@Entity"))
                return;

            // Naive parsing for:
            // @Entity
            // class User (
            // @Id val id: Long,
            // val name: String?
            // )

            Pattern classPattern = Pattern.compile("class\\s+(\\w+).*\\("); // Simple constructor style
            Matcher cm = classPattern.matcher(content);

            if (cm.find()) {
                String tableName = cm.group(1);
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // Parse fields inside parentheses
                // This is very limited regex parsing, assuming verify compact Kotlin syntax
                // Better would be finding the block between ( and )

                int start = content.indexOf("(");
                int end = content.lastIndexOf(")");
                if (start > -1 && end > start) {
                    String fieldsBlock = content.substring(start + 1, end);
                    String[] lines = fieldsBlock.split(",");

                    for (String line : lines) {
                        line = line.trim();
                        // val name: String
                        Pattern fieldPattern = Pattern.compile("(var|val)\\s+(\\w+)\\s*:\\s*([\\w<>]+)(\\?)?");
                        Matcher fm = fieldPattern.matcher(line);
                        if (fm.find()) {
                            String name = fm.group(2);
                            String type = fm.group(3);
                            boolean isNullable = fm.group(4) != null;
                            boolean isPk = line.contains("@Id");

                            if (isPk)
                                pks.add(name);

                            columns.add(ColumnMetadata.builder()
                                    .name(name)
                                    .dataType(mapKotlinType(type))
                                    .isPrimaryKey(isPk)
                                    .isNullable(isNullable)
                                    .build());
                        }
                    }
                }

                if (!columns.isEmpty()) {
                    tables.add(TableMetadata.builder()
                            .tableName(tableName)
                            .columns(columns)
                            .primaryKeys(pks)
                            .rowCount(0L)
                            .build());
                }
            }

        } catch (IOException e) {
            log.warn("Failed to parse kotlin file: {}", path);
        }
    }

    private String mapKotlinType(String type) {
        switch (type) {
            case "String":
                return "VARCHAR";
            case "Int":
            case "Long":
                return "INTEGER";
            case "Double":
                return "FLOAT";
            case "Boolean":
                return "BOOLEAN";
            case "LocalDateTime":
            case "LocalDate":
                return "TIMESTAMP";
            default:
                return "VARCHAR";
        }
    }
}
