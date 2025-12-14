package com.itdg.analyzer.service;

import com.itdg.analyzer.service.parser.JPAProjectParser;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { SourceHelperService.class, ProjectAnalysisService.class, JPAProjectParser.class })
public class GitAnalysisVerificationTest {

    @Autowired
    private SourceHelperService sourceHelperService;

    @Autowired
    private ProjectAnalysisService projectAnalysisService;

    @Test
    public void testGDDRepoAnalysis() {
        String gitUrl = "https://github.com/sukh115/GDD";
        System.out.println("Starting analysis verification for: " + gitUrl);

        File tempDir = null;
        try {
            // 1. Clone
            tempDir = sourceHelperService.cloneRepository(gitUrl);
            assertNotNull(tempDir);
            assertTrue(tempDir.exists());
            System.out.println("Repository cloned to: " + tempDir.getAbsolutePath());

            // 2. Analyze
            SchemaMetadata metadata = projectAnalysisService.analyzeProject(tempDir);

            // 3. Verify
            assertNotNull(metadata);
            System.out.println("Detected Language: " + metadata.getProjectInfo().getLanguage());
            System.out.println("Total Files: " + metadata.getProjectInfo().getTotalFiles());

            List<TableMetadata> tables = metadata.getTables();
            assertFalse(tables.isEmpty(), "Tables should be detected");

            System.out.println("\n=== Detected Tables ===");
            for (TableMetadata table : tables) {
                System.out.println("Table: " + table.getTableName());
                table.getColumns()
                        .forEach(col -> System.out.println("  - " + col.getName() + " (" + col.getDataType() + ")"));
            }

        } finally {
            if (tempDir != null) {
                sourceHelperService.cleanup(tempDir);
            }
        }
    }
}
