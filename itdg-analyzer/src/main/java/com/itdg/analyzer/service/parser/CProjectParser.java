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
public class CProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> {
                String s = p.toString();
                return s.endsWith(".c") || s.endsWith(".cpp") || s.endsWith(".h");
            });
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("C/C++")
                .framework("Embedded SQL")
                .analyzedFiles(0)
                .totalFiles(countCFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countCFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> {
                String s = p.toString();
                return s.endsWith(".c") || s.endsWith(".cpp") || s.endsWith(".h");
            }).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> {
                String s = p.toString();
                return s.endsWith(".c") || s.endsWith(".cpp") || s.endsWith(".h");
            }).forEach(path -> parseCFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing C project", e);
        }
        return tables;
    }

    private void parseCFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);
            // Search for "CREATE TABLE table ( ... )" strings
            // Pattern: "CREATE TABLE\\s+(\\w+)\\s*\\(([^;]+)\\)"
            // Case insensitive pattern for sql

            Pattern tablePattern = Pattern.compile("CREATE TABLE\\s+(\\w+)\\s*\\(([^;]+)\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher tm = tablePattern.matcher(content);

            while (tm.find()) {
                String tableName = tm.group(1);
                String body = tm.group(2);

                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // Split body by comma, handle basic definitions: "id INT PRIMARY KEY, name
                // VARCHAR(20)"
                String[] defs = body.split(",");
                for (String def : defs) {
                    def = def.trim();
                    if (def.isEmpty())
                        continue;

                    // Simple parsing: first word is name, second is type
                    String[] parts = def.split("\\s+");
                    if (parts.length >= 2) {
                        String name = parts[0];
                        String type = parts[1];
                        boolean isPk = def.toUpperCase().contains("PRIMARY KEY");

                        if (isPk)
                            pks.add(name);

                        columns.add(ColumnMetadata.builder()
                                .name(name)
                                .dataType(type.toUpperCase())
                                .isPrimaryKey(isPk)
                                .isNullable(!def.toUpperCase().contains("NOT NULL"))
                                .build());
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
            log.warn("Failed to parse C file: {}", path);
        }
    }
}
