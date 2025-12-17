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
 * Ruby on Rails (ActiveRecord) 프로젝트 파서
 * - db/migrate/*.rb 마이그레이션 파일 분석
 * - app/models/*.rb 모델 파일 분석
 */
@Slf4j
@Component
public class RubyRailsParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().contains("db/migrate") && p.toString().endsWith(".rb"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Ruby")
                .framework("Rails (ActiveRecord)")
                .analyzedFiles(0)
                .totalFiles(countMigrationFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countMigrationFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().contains("db/migrate") && p.toString().endsWith(".rb")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().contains("db/migrate") && p.toString().endsWith(".rb"))
                    .forEach(path -> parseMigrationFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Rails project", e);
        }
        return tables;
    }

    private void parseMigrationFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);

            // create_table :table_name do |t| ... end
            Pattern tablePattern = Pattern.compile("create_table\\s+:(\\w+).*?do\\s*\\|\\w+\\|(.+?)end",
                    Pattern.DOTALL);
            Matcher tm = tablePattern.matcher(content);

            while (tm.find()) {
                String tableName = tm.group(1);
                String body = tm.group(2);

                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // t.string :name
                // t.integer :age
                Pattern fieldPattern = Pattern.compile("t\\.(\\w+)\\s+:(\\w+)");
                Matcher fm = fieldPattern.matcher(body);

                // Add default id column
                pks.add("id");
                columns.add(ColumnMetadata.builder()
                        .name("id")
                        .dataType("BIGINT")
                        .isPrimaryKey(true)
                        .isAutoIncrement(true)
                        .build());

                while (fm.find()) {
                    String type = fm.group(1);
                    String name = fm.group(2);

                    columns.add(ColumnMetadata.builder()
                            .name(name)
                            .dataType(mapRailsType(type))
                            .isPrimaryKey(false)
                            .isNullable(true)
                            .build());
                }

                if (columns.size() > 1) {
                    tables.add(TableMetadata.builder()
                            .tableName(tableName)
                            .columns(columns)
                            .primaryKeys(pks)
                            .rowCount(0L)
                            .build());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to parse Rails migration: {}", path);
        }
    }

    private String mapRailsType(String type) {
        switch (type.toLowerCase()) {
            case "string":
            case "text":
                return "VARCHAR";
            case "integer":
                return "INTEGER";
            case "bigint":
                return "BIGINT";
            case "boolean":
                return "BOOLEAN";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP";
            case "date":
                return "DATE";
            case "float":
            case "decimal":
                return "FLOAT";
            case "binary":
                return "BLOB";
            case "json":
            case "jsonb":
                return "JSON";
            default:
                return "VARCHAR";
        }
    }
}
