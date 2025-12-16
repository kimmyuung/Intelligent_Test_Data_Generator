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
public class GoProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith(".go"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Go")
                .framework("GORM (Assumed)")
                .analyzedFiles(0)
                .totalFiles(countGoFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countGoFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".go")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".go"))
                    .forEach(path -> parseGoFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Go project", e);
        }
        return tables;
    }

    private void parseGoFile(Path path, List<TableMetadata> tables) {
        try {
            // Naive Regex for Go struct
            // type User struct {
            // Name string
            // Age int
            // }
            List<String> lines = Files.readAllLines(path);
            String currentStruct = null;
            List<ColumnMetadata> currentColumns = new ArrayList<>();
            boolean insideStruct = false;

            Pattern structStart = Pattern.compile("type\\s+(\\w+)\\s+struct\\s+\\{");
            Pattern fieldPattern = Pattern.compile("\\s+(\\w+)\\s+([\\w\\[\\]\\*]+).*"); // Field Type Tags

            for (String line : lines) {
                line = line.trim();

                if (!insideStruct) {
                    Matcher m = structStart.matcher(line);
                    if (m.find()) {
                        currentStruct = m.group(1);
                        insideStruct = true;
                        currentColumns = new ArrayList<>();
                        // Add default gorm.Model fields if embedded? Complex to detect.
                        // Assuming simple structs for now.
                    }
                } else {
                    if (line.equals("}")) {
                        if (currentStruct != null && !currentColumns.isEmpty()) {
                            tables.add(buildTable(currentStruct, currentColumns));
                        }
                        insideStruct = false;
                        currentStruct = null;
                        continue;
                    }

                    Matcher fm = fieldPattern.matcher(line);
                    if (fm.find()) {
                        String name = fm.group(1);
                        String type = fm.group(2);

                        // Ignore functions or complex types or embeddings (e.g. gorm.Model)
                        if (type.contains(".")) {
                            if (type.equals("gorm.Model")) {
                                // Add standard ID, CreatedAt, etc.
                                currentColumns.add(createCol("ID", "uint", true));
                                currentColumns.add(createCol("CreatedAt", "time.Time", false));
                                currentColumns.add(createCol("UpdatedAt", "time.Time", false));
                                currentColumns.add(createCol("DeletedAt", "gorm.DeletedAt", false));
                            }
                            continue;
                        }

                        currentColumns.add(createCol(name, type, name.equalsIgnoreCase("ID")));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to parse go file: {}", path);
        }
    }

    private ColumnMetadata createCol(String name, String type, boolean isPk) {
        return ColumnMetadata.builder()
                .name(name)
                .dataType(mapGoType(type))
                .isPrimaryKey(isPk)
                .isNullable(false) // Go zero value default
                .build();
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

    private String mapGoType(String type) {
        type = type.replace("*", "");
        switch (type) {
            case "string":
                return "VARCHAR";
            case "int":
            case "int8":
            case "int16":
            case "int32":
            case "int64":
            case "uint":
                return "INTEGER";
            case "bool":
                return "BOOLEAN";
            case "float32":
            case "float64":
                return "FLOAT";
            case "Time":
            case "time.Time":
                return "TIMESTAMP";
            default:
                return "VARCHAR";
        }
    }
}
