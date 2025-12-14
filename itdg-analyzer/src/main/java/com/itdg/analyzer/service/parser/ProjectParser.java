package com.itdg.analyzer.service.parser;

import com.itdg.common.dto.metadata.ProjectInfo;
import com.itdg.common.dto.metadata.TableMetadata;

import java.io.File;
import java.util.List;

public interface ProjectParser {

    /**
     * 해당 디렉토리가 이 파서로 분석 가능한지 확인합니다.
     */
    boolean supports(File projectDir);

    /**
     * 프로젝트에서 테이블 스키마 정보를 추출합니다.
     */
    List<TableMetadata> parse(File projectDir);

    /**
     * 프로젝트의 언어 및 프레임워크 정보를 감지합니다.
     */
    ProjectInfo detectInfo(File projectDir);
}
