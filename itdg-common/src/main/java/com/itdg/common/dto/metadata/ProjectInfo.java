package com.itdg.common.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInfo {
    private String language; // Java, SQL, Unknown
    private String framework; // Spring Boot, JPA, etc.
    private int totalFiles;
    private int analyzedFiles;
    private List<String> detectedFiles; // 분석에 사용된 주요 파일 목록
}
