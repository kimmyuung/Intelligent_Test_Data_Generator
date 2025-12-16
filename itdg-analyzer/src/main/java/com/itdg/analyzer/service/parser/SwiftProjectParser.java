package com.itdg.analyzer.service.parser;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.TableMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class SwiftProjectParser implements ProjectParser {

    @Override
    public boolean supports(File projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            return walk.anyMatch(p -> p.toString().endsWith(".xcdatamodeld"));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public ProjectInfo detectInfo(File projectDir) {
        return ProjectInfo.builder()
                .language("Swift")
                .framework("CoreData")
                .analyzedFiles(0)
                .totalFiles(countSwiftFiles(projectDir))
                .detectedFiles(new ArrayList<>())
                .build();
    }

    private int countSwiftFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return (int) walk.filter(p -> p.toString().endsWith(".swift")).count();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TableMetadata> parse(File projectDir) {
        List<TableMetadata> tables = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectDir.toPath())) {
            walk.filter(p -> p.toString().endsWith("contents") && p.getParent().toString().endsWith(".xcdatamodel"))
                    .forEach(path -> parseCoreDataXml(path, tables));
        } catch (IOException e) {
            log.error("Error parsing Swift project", e);
        }
        return tables;
    }

    private void parseCoreDataXml(Path path, List<TableMetadata> tables) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(path.toFile());
            doc.getDocumentElement().normalize();

            NodeList entityNodes = doc.getElementsByTagName("entity");

            for (int i = 0; i < entityNodes.getLength(); i++) {
                Element entityElement = (Element) entityNodes.item(i);
                String tableName = entityElement.getAttribute("name");
                List<ColumnMetadata> columns = new ArrayList<>();
                List<String> pks = new ArrayList<>(); // CoreData uses internal objectID, but we can look for custom one

                NodeList attributeNodes = entityElement.getElementsByTagName("attribute");
                for (int j = 0; j < attributeNodes.getLength(); j++) {
                    Element attrElement = (Element) attributeNodes.item(j);
                    String name = attrElement.getAttribute("name");
                    String type = attrElement.getAttribute("attributeType");
                    String optional = attrElement.getAttribute("optional");

                    columns.add(ColumnMetadata.builder()
                            .name(name)
                            .dataType(mapCoreDataType(type))
                            .isNullable("YES".equalsIgnoreCase(optional))
                            .isPrimaryKey(false) // CoreData manages ID internally
                            .build());
                }

                // Add implicit ID if needed for generation
                columns.add(0, ColumnMetadata.builder()
                        .name("objectID")
                        .dataType("VARCHAR")
                        .isPrimaryKey(true)
                        .build());
                pks.add("objectID");

                tables.add(TableMetadata.builder()
                        .tableName(tableName)
                        .columns(columns)
                        .primaryKeys(pks)
                        .rowCount(0L)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse CoreData XML: {}", path);
        }
    }

    private String mapCoreDataType(String type) {
        switch (type) {
            case "String":
                return "VARCHAR";
            case "Integer 16":
            case "Integer 32":
            case "Integer 64":
                return "INTEGER";
            case "Boolean":
                return "BOOLEAN";
            case "Float":
            case "Double":
                return "FLOAT";
            case "Date":
                return "TIMESTAMP";
            case "UUID":
                return "VARCHAR";
            case "Binary":
                return "BLOB";
            default:
                return "VARCHAR";
        }
    }
}
