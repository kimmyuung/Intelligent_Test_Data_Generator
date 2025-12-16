package com.itdg.analyzer.service.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class JPAProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        // 간단한 체크: pom.xml이나 build.gradle이 있거나 .java 파일이 존재하면 지원
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith(".java"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        int javaFileCount = 0;
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            javaFileCount = (int) walk.filter(p -> p.toString().endsWith(".java")).count();
        } catch (Exception e) {
            // ignore
        }

        return ProjectInfo.builder()
                .language("Java")
                .framework("Spring Data JPA") // 상세 감지는 추후 고도화
                .totalFiles(javaFileCount)
                .analyzedFiles(0) // parse() 후에 업데이트
                .detectedFiles(new ArrayList<>())
                .build();
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        // SymbolResolver 제거: AST 파싱만으로 충분함
        JavaParser javaParser = new JavaParser();

        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            List<File> javaFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File file : javaFiles) {
                try {
                    ParseResult<CompilationUnit> result = javaParser.parse(file);
                    if (result.isSuccessful() && result.getResult().isPresent()) {
                        CompilationUnit cu = result.getResult().get();
                        extractEntities(cu, tables);
                    }
                } catch (IOException e) {
                    log.warn("Failed to parse java file: {}", file.getName());
                }
            }
        } catch (IOException e) {
            log.error("Error walking project directory", e);
        }

        return tables;
    }

    private void extractEntities(CompilationUnit cu, List<TableMetadata> tables) {
        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (clazz.isAnnotationPresent("Entity")) {
                String tableName = getTableName(clazz);
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> primaryKeys = new ArrayList<>();

                for (FieldDeclaration field : clazz.getFields()) {
                    // @Transient 무시
                    if (field.isAnnotationPresent("Transient"))
                        continue;

                    String columnName = getColumnName(field);
                    String dataType = field.getElementType().asString();
                    boolean isPk = field.isAnnotationPresent("Id");
                    boolean isNullable = isNullable(field);

                    boolean isForeignKey = false;
                    String targetTable = null;

                    if (field.isAnnotationPresent("ManyToOne") || field.isAnnotationPresent("OneToOne")) {
                        isForeignKey = true;
                        // Simplistic assumption: field type name = table name (often true in this
                        // project context or acceptable mock)
                        // Or try to resolve generic type if collection (OneToMany) - but usually
                        // OneToMany is not the owner side.
                        // For ManyToOne, the field type (e.g. "User") is the target entity.
                        targetTable = field.getElementType().asString();

                        // Check if @JoinColumn override exists
                        Optional<AnnotationExpr> joinCol = field.getAnnotationByName("JoinColumn");
                        if (joinCol.isPresent() && joinCol.get().isNormalAnnotationExpr()) {
                            for (var pair : joinCol.get().asNormalAnnotationExpr().getPairs()) {
                                if (pair.getNameAsString().equals("name")) {
                                    columnName = pair.getValue().toString().replace("\"", "");
                                }
                            }
                        } else {
                            // Default convention: entity_id
                            columnName = field.getVariable(0).getNameAsString() + "_id";
                        }

                        // FKs are usually Long/Integer
                        dataType = "BIGINT";
                    }

                    if (isPk) {
                        primaryKeys.add(columnName);
                    }

                    columns.add(ColumnMetadata.builder()
                            .name(columnName)
                            .dataType(mapJavaTypeToSql(dataType))
                            .isPrimaryKey(isPk)
                            .isNullable(isNullable)
                            .isForeignKey(isForeignKey)
                            .foreignKeyTargetTable(targetTable)
                            .length(getColumnLength(field))
                            .build());
                }

                tables.add(TableMetadata.builder()
                        .tableName(tableName)
                        .columns(columns)
                        .primaryKeys(primaryKeys)
                        .rowCount(0L) // 초기 0
                        .build());
            }
        }
    }

    private String getTableName(ClassOrInterfaceDeclaration clazz) {
        Optional<AnnotationExpr> tableAnn = clazz.getAnnotationByName("Table");
        if (tableAnn.isPresent() && tableAnn.get().isNormalAnnotationExpr()) {
            // @Table(name="users") -> extract "users"
            for (var pair : tableAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("name")) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }
        return clazz.getNameAsString(); // Default to class name
    }

    private String getColumnName(FieldDeclaration field) {
        Optional<AnnotationExpr> colAnn = field.getAnnotationByName("Column");
        if (colAnn.isPresent() && colAnn.get().isNormalAnnotationExpr()) {
            for (var pair : colAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("name")) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }
        return field.getVariable(0).getNameAsString();
    }

    private boolean isNullable(FieldDeclaration field) {
        // 프리미티브 타입은 not null
        if (field.getElementType().isPrimitiveType())
            return false;

        Optional<AnnotationExpr> colAnn = field.getAnnotationByName("Column");
        if (colAnn.isPresent() && colAnn.get().isNormalAnnotationExpr()) {
            for (var pair : colAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("nullable")) {
                    return Boolean.parseBoolean(pair.getValue().toString());
                }
            }
        }
        return true; // Reference types default nullable
    }

    private Integer getColumnLength(FieldDeclaration field) {
        Optional<AnnotationExpr> colAnn = field.getAnnotationByName("Column");
        if (colAnn.isPresent() && colAnn.get().isNormalAnnotationExpr()) {
            for (var pair : colAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("length")) {
                    try {
                        return Integer.parseInt(pair.getValue().toString());
                    } catch (NumberFormatException e) {
                        return 255;
                    }
                }
            }
        }
        return 255; // Default length
    }

    private String mapJavaTypeToSql(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string":
                return "VARCHAR";
            case "long":
                return "BIGINT";
            case "integer":
            case "int":
                return "INTEGER";
            case "boolean":
                return "BOOLEAN";
            case "localdatetime":
            case "date":
                return "TIMESTAMP";
            default:
                return "VARCHAR";
        }
    }
}
