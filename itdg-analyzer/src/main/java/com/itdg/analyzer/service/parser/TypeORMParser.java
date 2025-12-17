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
 * TypeORM (TypeScript/JavaScript) 프로젝트 파서
 * - @Entity 데코레이터가 있는 클래스 분석
 */
@Slf4j
@Component
public class TypeORMParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".js"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("@Entity") && content.contains("typeorm");
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
                .language("TypeScript")
                .framework("TypeORM")
                .analyzedFiles(0)
                .totalFiles(countTsFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countTsFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".js")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".js"))
                    .forEach(path -> parseTsFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing TypeORM project", e);
        }
        return tables;
    }

    private void parseTsFile(Path path, List<TableMetadata> tables) {
        try {
            String content = Files.readString(path);

            if (!content.contains("@Entity"))
                return;

            // @Entity("users") 또는 @Entity() class User { ... }
            Pattern entityPattern = Pattern.compile(
                    "@Entity\\s*\\(([^)]*)\\)\\s*(?:export\\s+)?class\\s+(\\w+)\\s*(?:extends\\s+\\w+)?\\s*\\{",
                    Pattern.DOTALL);

            Matcher em = entityPattern.matcher(content);
            while (em.find()) {
                String entityOptions = em.group(1);
                String className = em.group(2);

                int classStart = em.end();
                int braceCount = 1;
                int classEnd = classStart;

                // 중괄호 매칭으로 클래스 끝 찾기
                for (int i = classStart; i < content.length() && braceCount > 0; i++) {
                    if (content.charAt(i) == '{')
                        braceCount++;
                    if (content.charAt(i) == '}')
                        braceCount--;
                    classEnd = i;
                }

                String classBody = content.substring(classStart, classEnd);
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                // 테이블 이름 추출
                String tableName = className;
                Matcher tnm = Pattern.compile("['\"]([^'\"]+)['\"]").matcher(entityOptions);
                if (tnm.find()) {
                    tableName = tnm.group(1);
                }

                // @PrimaryGeneratedColumn()
                // id: number;
                Pattern pkColPattern = Pattern.compile("@PrimaryGeneratedColumn\\(\\)\\s*(\\w+)\\s*:");
                Matcher pkm = pkColPattern.matcher(classBody);
                while (pkm.find()) {
                    String colName = pkm.group(1);
                    pks.add(colName);
                    columns.add(ColumnMetadata.builder()
                            .name(colName)
                            .dataType("BIGINT")
                            .isPrimaryKey(true)
                            .isAutoIncrement(true)
                            .build());
                }

                // @Column()
                // name: string;
                Pattern colPattern = Pattern.compile("@Column\\s*\\(([^)]*)\\)\\s*(\\w+)\\s*:\\s*(\\w+)");
                Matcher cm = colPattern.matcher(classBody);

                while (cm.find()) {
                    String colOptions = cm.group(1);
                    String colName = cm.group(2);
                    String colType = cm.group(3);

                    columns.add(ColumnMetadata.builder()
                            .name(colName)
                            .dataType(mapTypeScriptType(colType))
                            .isPrimaryKey(false)
                            .isNullable(colOptions.contains("nullable: true"))
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
            log.warn("Failed to parse TypeORM file: {}", path);
        }
    }

    private String mapTypeScriptType(String type) {
        switch (type.toLowerCase()) {
            case "string":
                return "VARCHAR";
            case "number":
                return "INTEGER";
            case "boolean":
                return "BOOLEAN";
            case "date":
                return "TIMESTAMP";
            default:
                return "VARCHAR";
        }
    }
}
