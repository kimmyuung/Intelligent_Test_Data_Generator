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
 * Python SQLAlchemy 프로젝트 파서
 * - SQLAlchemy 모델 클래스 분석
 */
@Slf4j
@Component
public class PythonSQLAlchemyParser implements ProjectParser {

    // 클래스 정의 패턴: class User(Base): 또는 class User(db.Model):
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "class\\s+(\\w+)\\s*\\(.*(?:Base|Model).*\\):");

    // 컬럼 정의 패턴: name = Column(String(100))
    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "\\s+(\\w+)\\s*=\\s*(?:db\\.)?Column\\s*\\(\\s*(\\w+)");

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".py"))
                    .anyMatch(p -> {
                        try {
                            String content = Files.readString(p);
                            return content.contains("from sqlalchemy") ||
                                    content.contains("import sqlalchemy") ||
                                    (content.contains("Column(") && content.contains("Base"));
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
                .language("Python")
                .framework("SQLAlchemy")
                .analyzedFiles(0)
                .totalFiles(countPyFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countPyFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".py")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith(".py"))
                    .forEach(path -> parsePythonFile(path, tables));
        } catch (IOException e) {
            log.error("Error parsing SQLAlchemy project", e);
        }
        return tables;
    }

    private void parsePythonFile(Path path, List<TableMetadata> tables) {
        try {
            List<String> lines = Files.readAllLines(path);
            String content = String.join("\n", lines);

            if (!content.contains("Column("))
                return;

            Matcher cm = CLASS_PATTERN.matcher(content);
            while (cm.find()) {
                String className = cm.group(1);
                int classStart = cm.end();

                // 다음 클래스 또는 파일 끝까지
                int classEnd = content.length();
                Matcher nextClass = CLASS_PATTERN.matcher(content.substring(classStart));
                if (nextClass.find()) {
                    classEnd = classStart + nextClass.start();
                }

                String classBody = content.substring(classStart, classEnd);
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>();

                Matcher colMatcher = COLUMN_PATTERN.matcher(classBody);
                while (colMatcher.find()) {
                    String colName = colMatcher.group(1);
                    String colType = colMatcher.group(2);

                    // __tablename__ 등 제외
                    if (colName.startsWith("__"))
                        continue;

                    boolean isPk = classBody.contains(colName + " = Column") &&
                            classBody.substring(classBody.indexOf(colName)).contains("primary_key=True");

                    if (isPk)
                        pks.add(colName);

                    columns.add(ColumnMetadata.builder()
                            .name(colName)
                            .dataType(mapSQLAlchemyType(colType))
                            .isPrimaryKey(isPk)
                            .isNullable(true)
                            .build());
                }

                if (!columns.isEmpty()) {
                    // 테이블 이름 추출 시도
                    String tableName = className;
                    Pattern tableNamePattern = Pattern.compile("__tablename__\\s*=\\s*['\"]([^'\"]+)['\"]");
                    Matcher tnm = tableNamePattern.matcher(classBody);
                    if (tnm.find()) {
                        tableName = tnm.group(1);
                    }

                    tables.add(TableMetadata.builder()
                            .tableName(tableName)
                            .columns(columns)
                            .primaryKeys(pks)
                            .rowCount(0L)
                            .build());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to parse SQLAlchemy file: {}", path);
        }
    }

    private String mapSQLAlchemyType(String type) {
        switch (type) {
            case "String":
            case "Text":
                return "VARCHAR";
            case "Integer":
                return "INTEGER";
            case "BigInteger":
                return "BIGINT";
            case "Boolean":
                return "BOOLEAN";
            case "DateTime":
            case "TIMESTAMP":
                return "TIMESTAMP";
            case "Date":
                return "DATE";
            case "Time":
                return "TIME";
            case "Float":
            case "Numeric":
                return "FLOAT";
            case "LargeBinary":
                return "BLOB";
            case "JSON":
                return "JSON";
            default:
                return "VARCHAR";
        }
    }
}
