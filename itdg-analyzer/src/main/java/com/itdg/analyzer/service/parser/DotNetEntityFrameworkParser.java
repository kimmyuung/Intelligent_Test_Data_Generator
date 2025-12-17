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

/**
 * .NET Entity Framework / C# 프로젝트 파서
 * - DbContext 및 Entity 클래스 분석
 */
@Slf4j
@Component
public class DotNetEntityFrameworkParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".cs"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("DbContext") || content.contains("DbSet<");
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("C#")
                .framework("Entity Framework")
                .analyzedFiles(0)
                .totalFiles(countCsFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countCsFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".cs")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".cs"))
                    .forEach(path -> parseCsFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Entity Framework project", e);
        }
        return tables;
    }

    private void parseCsFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);

            // public class User { ... } 패턴
            Pattern classPattern = Pattern.compile(
                    "public\\s+class\\s+(\\w+)\\s*(?::\\s*\\w+)?\\s*\\{([^}]+)\\}",
                    Pattern.DOTALL);

            Matcher cm = classPattern.matcher(content);
            while (cm.find()) {
                String className = cm.group(1);
                String classBody = cm.group(2);

                // DbContext 클래스는 건너뜀
                if (classBody.contains("DbSet<"))
                    continue;

                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // public int Id { get; set; }
                // public string Name { get; set; }
                Pattern propPattern = Pattern.compile(
                        "public\\s+(\\w+)\\??\\s+(\\w+)\\s*\\{\\s*get;");

                Matcher pm = propPattern.matcher(classBody);
                while (pm.find()) {
                    String type = pm.group(1);
                    String name = pm.group(2);

                    boolean isPk = name.equalsIgnoreCase("Id") ||
                            name.equalsIgnoreCase(className + "Id") ||
                            classBody.contains("[Key]") && classBody.indexOf("[Key]") < classBody.indexOf(name);

                    if (isPk)
                        pks.add(name);

                    columns.add(ColumnMetadata.builder()
                            .name(name)
                            .dataType(mapCSharpType(type))
                            .isPrimaryKey(isPk)
                            .isNullable(type.endsWith("?") || !isPrimitiveType(type))
                            .build());
                }

                if (!columns.isEmpty()) {
                    tables.add(TableMetadata.builder()
                            .tableName(className)
                            .columns(columns)
                            .primaryKeys(pks)
                            .rowCount(0L)
                            .build());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to parse C# file: {}", path);
        }
    }

    private boolean isPrimitiveType(String type) {
        return List.of("int", "long", "bool", "float", "double", "decimal", "byte", "short")
                .contains(type.replace("?", ""));
    }

    private String mapCSharpType(String type) {
        String t = type.replace("?", "");
        switch (t.toLowerCase()) {
            case "string":
                return "VARCHAR";
            case "int":
            case "int32":
                return "INTEGER";
            case "long":
            case "int64":
                return "BIGINT";
            case "bool":
            case "boolean":
                return "BOOLEAN";
            case "datetime":
            case "datetimeoffset":
                return "TIMESTAMP";
            case "dateonly":
                return "DATE";
            case "timeonly":
                return "TIME";
            case "float":
            case "double":
            case "decimal":
                return "FLOAT";
            case "byte[]":
                return "BLOB";
            case "guid":
                return "VARCHAR";
            default:
                return "VARCHAR";
        }
    }
}
