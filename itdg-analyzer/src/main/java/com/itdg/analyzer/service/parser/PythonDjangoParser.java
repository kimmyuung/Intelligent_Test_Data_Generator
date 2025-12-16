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
public class PythonDjangoParser implements ProjectParser {

    // Regex to match class definition inheriting from models.Model
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+(\\w+)\\s*\\(.*models\\.Model.*\\):");
    // Regex to match fields: name = models.CharField(...)
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\s+(\\w+)\\s*=\\s*models\\.(\\w+)Field");

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith("models.py") ||
                    (p.toString().endsWith(".py") && containsDjangoImport(p)));
        } catch (IOException e) {
            return false;
        }
    }

    private boolean containsDjangoImport(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("from django.db import models") || content.contains("from django.db");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Python")
                .framework("Django")
                .analyzedFiles(0)
                .totalFiles(countFiles(projectDir, ".py"))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countFiles(File dir, String ext) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(ext)).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".py"))
                    .forEach(path -> parseFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Python project", e);
        }
        return tables;
    }

    private void parseFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);
            Matcher classMatcher = CLASS_PATTERN.matcher(content);

            // Naive parsing: Assume file contains classes sequentially
            // Better: split by class or line-by-line state machine.
            // For simplicity in regex:
            // We'll iterate line by line to track class context.

            List<String> lines = Files.readAllLines(path);
            String currentTable = null;
            List<ColumnMetadata> currentColumns = new ArrayList<>();

            for (String line : lines) {
                Matcher cm = CLASS_PATTERN.matcher(line);
                if (cm.find()) {
                    if (currentTable != null) {
                        tables.add(buildTable(currentTable, currentColumns));
                        currentColumns = new ArrayList<>();
                    }
                    currentTable = cm.group(1);
                    continue;
                }

                if (currentTable != null) {
                    Matcher fm = FIELD_PATTERN.matcher(line);
                    if (fm.find()) {
                        String colName = fm.group(1);
                        String djangoType = fm.group(2);
                        currentColumns.add(ColumnMetadata.builder()
                                .name(colName)
                                .dataType(mapDjangoType(djangoType))
                                .isNullable(line.contains("null=True"))
                                .isPrimaryKey(line.contains("primary_key=True") || colName.equals("id"))
                                .length(255)
                                .build());
                    }
                }
            }
            if (currentTable != null) {
                tables.add(buildTable(currentTable, currentColumns));
            }

        } catch (IOException e) {
            log.warn("Failed to read python file: {}", path);
        }
    }

    private TableMetadata buildTable(String name, List<ColumnMetadata> cols) {
        // Django implicit ID if not present
        boolean hasPk = cols.stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsPrimaryKey()));
        if (!hasPk) {
            cols.add(0, ColumnMetadata.builder()
                    .name("id")
                    .dataType("BIGINT")
                    .isPrimaryKey(true)
                    .isAutoIncrement(true)
                    .build());
        }

        List<String> pks = new ArrayList<>();
        cols.stream().filter(c -> Boolean.TRUE.equals(c.getIsPrimaryKey())).forEach(c -> pks.add(c.getName()));

        return TableMetadata.builder()
                .tableName(name) // Django usually converts to snake_case app_model, but keeping class name is
                                 // fine for test data
                .columns(cols)
                .primaryKeys(pks)
                .rowCount(0L)
                .build();
    }

    private String mapDjangoType(String type) {
        switch (type) {
            case "Char":
                return "VARCHAR";
            case "Text":
                return "TEXT";
            case "Integer":
                return "INTEGER";
            case "BigInteger":
                return "BIGINT";
            case "Boolean":
                return "BOOLEAN";
            case "Date":
                return "DATE";
            case "DateTime":
                return "TIMESTAMP";
            case "Float":
                return "FLOAT";
            case "Decimal":
                return "DECIMAL";
            default:
                return "VARCHAR";
        }
    }
}
