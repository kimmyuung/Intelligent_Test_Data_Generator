package com.itdg.analyzer.service;

import com.itdg.analyzer.exception.AnalysisFailedException;
import com.itdg.analyzer.service.parser.ProjectParser;
import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.SchemaMetadata;
import com.itdg.common.dto.metadata.TableMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAnalysisService {

    private final List<ProjectParser> parsers;

    public SchemaMetadata analyzeProject(File projectDir) {
        log.info("Analyzing project directory: {}", projectDir.getAbsolutePath());

        // 지원하는 파서 찾기 (우선순위: Java -> SQL)
        // Spring이 List<ProjectParser> 주입 시 @Order 등을 따를 수 있음. 현재는 순서대로.

        ProjectParser selectedParser = parsers.stream()
                .filter(parser -> parser.supports(projectDir))
                .findFirst()
                .orElseThrow(() -> {
                    // 지원하는 파서가 없을 때, 어떤 언어인지 확인하여 상세 메시지 제공
                    String detectedType = detectProjectType(projectDir);
                    String supported = parsers.stream()
                            .map(p -> p.getClass().getSimpleName().replace("ProjectParser", ""))
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("None");
                    return new AnalysisFailedException(
                            "지원되지 않는 프로젝트 형식입니다. (" + detectedType + " 감지됨). 지원되는 형식: " + supported);
                });

        log.info("Selected parser: {}", selectedParser.getClass().getSimpleName());

        ProjectInfo projectInfo = selectedParser.detectInfo(projectDir);
        List<TableMetadata> tables = selectedParser.parse(projectDir);

        // 분석된 파일 수 업데이트
        // ProjectInfo는 불변 객체(Builder)이므로 새로 생성 필요하다면 여기서 처리
        // 일단은 detectInfo에서 대략적인 전체 파일 수만 가져옴.

        if (tables.isEmpty()) {
            throw new AnalysisFailedException("분석 가능한 테이블(Entity/DDL)을 찾을 수 없습니다.");
        }

        return SchemaMetadata.builder()
                .databaseName("Project: " + projectInfo.getLanguage())
                .tables(tables)
                .analyzedAt(LocalDateTime.now())
                .projectInfo(projectInfo)
                .build();
    }

    private String detectProjectType(File projectDir) {
        if (new File(projectDir, "package.json").exists())
            return "JavaScript/Node.js";
        if (new File(projectDir, "requirements.txt").exists())
            return "Python";
        if (new File(projectDir, "pom.xml").exists() || new File(projectDir, "build.gradle").exists())
            return "Java (Entity 없음)";
        return "Unknown";
    }
}
