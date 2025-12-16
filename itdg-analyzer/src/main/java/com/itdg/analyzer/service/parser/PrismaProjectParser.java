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
public class PrismaProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith("schema.prisma"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Node.js/TypeScript")
                .framework("Prisma")
                .analyzedFiles(0)
                .totalFiles(1) // Usually one schema file
                .detectedFiles(new ArrayList<>())
                .build();
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith("schema.prisma"))
                    .forEach(path -> parsePrismaFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Prisma schema", e);
        }
        return tables;
    }

    private void parsePrismaFile(Path path, List<TableMetadata> tables) {
        try {
            List<String> lines = Files.readAllLines(path);
            String currentModel = null;
            List<ColumnMetadata> currentColumns = new ArrayList<>();
            Pattern modelStart = Pattern.compile("model\\s+(\\w+)\\s+\\{");
            Pattern fieldPattern = Pattern.compile("\\s+(\\w+)\\s+(\\w+)(.*)");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//"))
                    continue;

                Matcher mm = modelStart.matcher(line);
                if (mm.find()) {
                    if (currentModel != null) {
                        tables.add(buildTable(currentModel, currentColumns));
                        currentColumns = new ArrayList<>();
                    }
                    currentModel = mm.group(1);
                    continue;
                }

                if (currentModel != null) {
                    if (line.equals("}")) {
                        tables.add(buildTable(currentModel, currentColumns));
                        currentModel = null;
                        currentColumns = new ArrayList<>();
                        continue;
                    }

                    Matcher fm = fieldPattern.matcher(line);
                    if (fm.find()) {
                        String name = fm.group(1);
                        String type = fm.group(2);
                        String attributes = fm.group(3);

                        // Skip relation fields (capitalized type usually means another model)
                        // Simple heuristic: Prisma basic types are String, Int, DateTime, Boolean,
                        // Float, Decimal, BigInt, Bytes, Json
                        if (!isPrismaScalar(type))
                            continue;

                        currentColumns.add(ColumnMetadata.builder()
                                .name(name)
                                .dataType(mapPrismaType(type))
                                .isPrimaryKey(attributes.contains("@id"))
                                .isNullable(type.endsWith("?") || attributes.contains("?"))
                                .isAutoIncrement(attributes.contains("autoincrement()"))
                                .build());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read prisma file: {}", path);
        }
    }

    private TableMetadata buildTable(String name, List<ColumnMetadata> cols) {
        List<String> pks = new ArrayList<>();
        cols.stream().filter(c -> Boolean.TRUE.equals(c.getIsPrimaryKey())).forEach(c -> pks.add(c.getName()));
        return TableMetadata.builder()
                .tableName(name)
                .columns(cols)
                .primaryKeys(pks)
                .rowCount(0L)
                .build();
    }

    private boolean isPrismaScalar(String type) {
        String t = type.replace("?", "").replace("[]", "");
        return List.of("String", "Boolean", "Int", "BigInt", "Float", "Decimal", "DateTime", "Json", "Bytes")
                .contains(t);
    }

    private String mapPrismaType(String type) {
        String t = type.replace("?", "").replace("[]", "");
        switch (t) {
            case "String":
                return "VARCHAR";
            case "Int":
                return "INTEGER";
            case "BigInt":
                return "BIGINT";
            case "Boolean":
                return "BOOLEAN";
            case "DateTime":
                return "TIMESTAMP";
            case "Float":
                return "FLOAT";
            case "Decimal":
                return "DECIMAL";
            case "Json":
                return "JSON";
            default:
                return "VARCHAR";
        }
    }
}
