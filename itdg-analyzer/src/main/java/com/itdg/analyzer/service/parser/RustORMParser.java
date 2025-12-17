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
 * Rust Diesel / SeaORM 프로젝트 파서
 * - diesel schema.rs 또는 SeaORM entity 분석
 */
@Slf4j
@Component
public class RustORMParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".rs"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("diesel::table!") ||
                                    content.contains("#[derive(DeriveEntityModel") ||
                                    content.contains("table_name =");
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
                .language("Rust")
                .framework("Diesel/SeaORM")
                .analyzedFiles(0)
                .totalFiles(countRsFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countRsFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".rs")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".rs"))
                    .forEach(path -> parseRsFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Rust ORM project", e);
        }
        return tables;
    }

    private void parseRsFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);

            // Diesel schema 파싱: diesel::table! { users (id) { ... } }
            Pattern dieselTablePattern = Pattern.compile(
                    "diesel::table!\\s*\\{\\s*(\\w+)\\s*\\(([^)]+)\\)\\s*\\{([^}]+)\\}",
                    Pattern.DOTALL);

            Matcher dm = dieselTablePattern.matcher(content);
            while (dm.find()) {
                String tableName = dm.group(1);
                String pkColumn = dm.group(2).trim();
                String columnsBody = dm.group(3);

                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = List.of(pkColumn.split(","));

                // id -> Integer,
                // name -> Varchar,
                Pattern colPattern = Pattern.compile("(\\w+)\\s*->\\s*(\\w+)");
                Matcher cm = colPattern.matcher(columnsBody);

                while (cm.find()) {
                    String colName = cm.group(1);
                    String colType = cm.group(2);

                    columns.add(ColumnMetadata.builder()
                            .name(colName)
                            .dataType(mapDieselType(colType))
                            .isPrimaryKey(pks.contains(colName))
                            .isNullable(!pks.contains(colName))
                            .build());
                }

                if (!columns.isEmpty()) {
                    tables.add(TableMetadata.builder()
                            .tableName(tableName)
                            .columns(columns)
                            .primaryKeys(new ArrayList<>(pks))
                            .rowCount(0L)
                            .build());
                }
            }

            // SeaORM Entity 파싱: pub struct Model { pub id: i32, pub name: String }
            Pattern seaormPattern = Pattern.compile(
                    "#\\[derive\\(.*DeriveEntityModel.*\\)\\].*?pub struct Model\\s*\\{([^}]+)\\}",
                    Pattern.DOTALL);

            Matcher sm = seaormPattern.matcher(content);
            while (sm.find()) {
                String body = sm.group(1);
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // Extract table_name from attribute if exists
                String tableName = "unknown";
                Pattern tableAttr = Pattern.compile("table_name\\s*=\\s*\"(\\w+)\"");
                Matcher tam = tableAttr.matcher(content);
                if (tam.find()) {
                    tableName = tam.group(1);
                }

                // pub id: i32,
                Pattern fieldPattern = Pattern.compile("pub\\s+(\\w+)\\s*:\\s*([^,]+)");
                Matcher fm = fieldPattern.matcher(body);

                while (fm.find()) {
                    String name = fm.group(1);
                    String type = fm.group(2).trim();

                    boolean isPk = name.equals("id") || content.contains("#[sea_orm(primary_key)]");
                    if (isPk)
                        pks.add(name);

                    columns.add(ColumnMetadata.builder()
                            .name(name)
                            .dataType(mapRustType(type))
                            .isPrimaryKey(isPk)
                            .isNullable(type.startsWith("Option<"))
                            .build());
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
            log.warn("Failed to parse Rust file: {}", path);
        }
    }

    private String mapDieselType(String type) {
        switch (type) {
            case "Varchar":
            case "Text":
                return "VARCHAR";
            case "Integer":
            case "Int4":
                return "INTEGER";
            case "BigInt":
            case "Int8":
                return "BIGINT";
            case "Bool":
                return "BOOLEAN";
            case "Timestamp":
            case "Timestamptz":
                return "TIMESTAMP";
            case "Date":
                return "DATE";
            case "Time":
                return "TIME";
            case "Float":
            case "Double":
            case "Numeric":
                return "FLOAT";
            case "Bytea":
                return "BLOB";
            case "Json":
            case "Jsonb":
                return "JSON";
            case "Uuid":
                return "VARCHAR";
            default:
                return "VARCHAR";
        }
    }

    private String mapRustType(String type) {
        String t = type.replace("Option<", "").replace(">", "").trim();
        switch (t) {
            case "String":
                return "VARCHAR";
            case "i32":
                return "INTEGER";
            case "i64":
                return "BIGINT";
            case "bool":
                return "BOOLEAN";
            case "DateTime":
            case "NaiveDateTime":
                return "TIMESTAMP";
            case "NaiveDate":
                return "DATE";
            case "f32":
            case "f64":
                return "FLOAT";
            case "Vec<u8>":
                return "BLOB";
            case "Uuid":
                return "VARCHAR";
            default:
                return "VARCHAR";
        }
    }
}
