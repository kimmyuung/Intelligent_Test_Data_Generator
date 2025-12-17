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
 * PHP Laravel (Eloquent) 프로젝트 파서
 * - database/migrations/*.php 마이그레이션 파일 분석
 */
@Slf4j
@Component
public class PHPLaravelParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().contains("database/migrations") && p.toString().endsWith(".php"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("PHP")
                .framework("Laravel (Eloquent)")
                .analyzedFiles(0)
                .totalFiles(countMigrationFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countMigrationFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().contains("database/migrations") && p.toString().endsWith(".php"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().contains("database/migrations") && p.toString().endsWith(".php"))
                    .forEach(path -> parseMigrationFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Laravel project", e);
        }
        return tables;
    }

    private void parseMigrationFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);

            // Schema::create('table_name', function (Blueprint $table) { ... });
            Pattern tablePattern = Pattern.compile(
                    "Schema::create\\s*\\(\\s*['\"]([^'\"]+)['\"].*?function.*?\\{(.+?)\\}\\)",
                    Pattern.DOTALL);
            Matcher tm = tablePattern.matcher(content);

            while (tm.find()) {
                String tableName = tm.group(1);
                String body = tm.group(2);

                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // $table->id();
                // $table->string('name');
                // $table->integer('age');
                Pattern fieldPattern = Pattern.compile("\\$table->(\\w+)\\s*\\(\\s*['\"]([^'\"]+)['\"]");
                Pattern idPattern = Pattern.compile("\\$table->id\\(\\)");

                Matcher idMatcher = idPattern.matcher(body);
                if (idMatcher.find()) {
                    pks.add("id");
                    columns.add(ColumnMetadata.builder()
                            .name("id")
                            .dataType("BIGINT")
                            .isPrimaryKey(true)
                            .isAutoIncrement(true)
                            .build());
                }

                Matcher fm = fieldPattern.matcher(body);
                while (fm.find()) {
                    String type = fm.group(1);
                    String name = fm.group(2);

                    // Skip timestamps(), etc.
                    if (name.equals("created_at") || name.equals("updated_at"))
                        continue;

                    columns.add(ColumnMetadata.builder()
                            .name(name)
                            .dataType(mapLaravelType(type))
                            .isPrimaryKey(false)
                            .isNullable(!body.contains("->nullable()"))
                            .build());
                }

                // Handle timestamps()
                if (body.contains("timestamps()")) {
                    columns.add(
                            ColumnMetadata.builder().name("created_at").dataType("TIMESTAMP").isNullable(true).build());
                    columns.add(
                            ColumnMetadata.builder().name("updated_at").dataType("TIMESTAMP").isNullable(true).build());
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
            log.warn("Failed to parse Laravel migration: {}", path);
        }
    }

    private String mapLaravelType(String type) {
        switch (type.toLowerCase()) {
            case "string":
            case "text":
            case "longtext":
            case "mediumtext":
                return "VARCHAR";
            case "integer":
            case "tinyinteger":
            case "smallinteger":
            case "mediuminteger":
                return "INTEGER";
            case "biginteger":
            case "unsignedbiginteger":
                return "BIGINT";
            case "boolean":
                return "BOOLEAN";
            case "datetime":
            case "timestamp":
            case "timestamptz":
                return "TIMESTAMP";
            case "date":
                return "DATE";
            case "time":
                return "TIME";
            case "float":
            case "double":
            case "decimal":
                return "FLOAT";
            case "binary":
                return "BLOB";
            case "json":
            case "jsonb":
                return "JSON";
            case "uuid":
                return "VARCHAR";
            default:
                return "VARCHAR";
        }
    }
}
