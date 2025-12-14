package com.itdg.analyzer.service.parser;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.TableMetadata;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class SQLProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith(".sql"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        int sqlFiles = 0;
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            sqlFiles = (int) walk.filter(p -> p.toString().endsWith(".sql")).count();
        } catch (Exception e) {
            // ignore
        }
        return ProjectInfo.builder()
                .language("SQL")
                .framework("DDL Script")
                .totalFiles(sqlFiles)
                .analyzedFiles(0)
                .detectedFiles(new ArrayList<>())
                .build();
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            List<File> sqlFiles = walk
                    .filter(p -> p.toString().endsWith(".sql"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File file : sqlFiles) {
                try {
                    String content = Files.readString(file.toPath());
                    // 멀티 스테이트먼트 지원을 위해 세미콜론 등으로 쪼개거나 전체 파싱 시도
                    // JSQLParser는 기본적으로 Statements 파싱 지원
                    // 여기서는 단순화를 위해 하나씩 시도하거나 전체를 구문분석

                    try {
                        net.sf.jsqlparser.statement.Statements statements = CCJSqlParserUtil.parseStatements(content);
                        for (Statement stmt : statements.getStatements()) {
                            if (stmt instanceof CreateTable) {
                                tables.add(extractTable((CreateTable) stmt));
                            }
                        }
                    } catch (Exception e) {
                        // 개별 SQL 파일 파싱 실패 시 로그만 남기고 진행
                        log.warn("Failed to parse SQL statements in file: {}", file.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to read SQL file: {}", file.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error walking project directory", e);
        }

        return tables;
    }

    private TableMetadata extractTable(CreateTable createTable) {
        String tableName = createTable.getTable().getName();
        List<ColumnMetadata> columns = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>(); // PK 파싱 로직 추가 필요 (복잡할 수 있음)

        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition colDef : createTable.getColumnDefinitions()) {
                String typeName = colDef.getColDataType().getDataType();
                boolean isPk = false;
                // PK 제약조건 확인 로직 (Index 등을 뒤져야 함 - 일단 생략하고 이름 관례나 추후 고도화)

                // 간단히 컬럼 스펙에서 PRIMARY KEY 있는지 확인 (JSQLParser 구조에 따라 다름)
                if (colDef.getColumnSpecs() != null) {
                    for (String spec : colDef.getColumnSpecs()) {
                        if ("PRIMARY".equalsIgnoreCase(spec) || "KEY".equalsIgnoreCase(spec)) {
                            isPk = true;
                        }
                    }
                }
                if (isPk)
                    primaryKeys.add(colDef.getColumnName());

                columns.add(ColumnMetadata.builder()
                        .name(colDef.getColumnName())
                        .dataType(typeName)
                        .isPrimaryKey(isPk)
                        .isNullable(true) // 기본값
                        .length(0) // 파싱 필요
                        .build());
            }
        }

        return TableMetadata.builder()
                .tableName(tableName)
                .columns(columns)
                .primaryKeys(primaryKeys)
                .rowCount(0L)
                .build();
    }
}
