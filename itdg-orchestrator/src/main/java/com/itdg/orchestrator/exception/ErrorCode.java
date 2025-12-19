package com.itdg.orchestrator.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 정의
 * 
 * 카테고리별로 세분화된 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========================================
    // 공통 에러 (1xxx)
    // ========================================
    INTERNAL_SERVER_ERROR(500, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(400, "COMMON_002", "유효하지 않은 입력입니다."),
    RESOURCE_NOT_FOUND(404, "COMMON_003", "리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(401, "COMMON_004", "인증이 필요합니다."),
    FORBIDDEN(403, "COMMON_005", "접근 권한이 없습니다."),

    // ========================================
    // 데이터베이스 연결 에러 (2xxx)
    // ========================================
    DATABASE_CONNECTION_FAILED(500, "DB_001", "데이터베이스 연결에 실패했습니다."),
    DATABASE_DRIVER_NOT_FOUND(500, "DB_002", "데이터베이스 드라이버를 찾을 수 없습니다."),
    DATABASE_AUTHENTICATION_FAILED(401, "DB_003", "데이터베이스 인증에 실패했습니다."),
    DATABASE_TIMEOUT(504, "DB_004", "데이터베이스 연결 시간 초과입니다."),
    INVALID_DATABASE_URL(400, "DB_005", "유효하지 않은 데이터베이스 URL입니다."),

    // ========================================
    // 스키마 분석 에러 (3xxx)
    // ========================================
    SCHEMA_EXTRACTION_FAILED(500, "SCHEMA_001", "스키마 추출에 실패했습니다."),
    SCHEMA_EMPTY(400, "SCHEMA_002", "추출된 스키마가 비어있습니다."),
    UNSUPPORTED_PROJECT_TYPE(400, "SCHEMA_003", "지원되지 않는 프로젝트 타입입니다."),
    TABLE_NOT_FOUND(404, "SCHEMA_004", "테이블을 찾을 수 없습니다."),
    COLUMN_NOT_FOUND(404, "SCHEMA_005", "컬럼을 찾을 수 없습니다."),

    // ========================================
    // Git 저장소 에러 (4xxx)
    // ========================================
    GIT_CLONE_FAILED(500, "GIT_001", "Git 저장소 클론에 실패했습니다."),
    GIT_INVALID_URL(400, "GIT_002", "유효하지 않은 Git URL입니다."),
    GIT_AUTHENTICATION_FAILED(401, "GIT_003", "Git 인증에 실패했습니다."),
    GIT_REPOSITORY_NOT_FOUND(404, "GIT_004", "Git 저장소를 찾을 수 없습니다."),

    // ========================================
    // 데이터 생성 에러 (5xxx)
    // ========================================
    DATA_GENERATION_FAILED(500, "GEN_001", "데이터 생성에 실패했습니다."),
    INVALID_ROW_COUNT(400, "GEN_002", "유효하지 않은 행 수입니다."),
    NO_TABLES_SELECTED(400, "GEN_003", "선택된 테이블이 없습니다."),
    STRATEGY_NOT_FOUND(500, "GEN_004", "적합한 데이터 생성 전략을 찾을 수 없습니다."),

    // ========================================
    // ML 서버 에러 (6xxx)
    // ========================================
    ML_SERVER_UNAVAILABLE(503, "ML_001", "ML 서버를 사용할 수 없습니다."),
    ML_MODEL_NOT_FOUND(404, "ML_002", "ML 모델을 찾을 수 없습니다."),
    ML_TRAINING_FAILED(500, "ML_003", "ML 모델 학습에 실패했습니다."),
    ML_GENERATION_FAILED(500, "ML_004", "ML 합성 데이터 생성에 실패했습니다."),
    ML_FILE_ANALYSIS_FAILED(400, "ML_005", "파일 분석에 실패했습니다."),
    ML_INSUFFICIENT_DATA(400, "ML_006", "학습 데이터가 부족합니다."),

    // ========================================
    // 파일 처리 에러 (7xxx)
    // ========================================
    FILE_NOT_FOUND(404, "FILE_001", "파일을 찾을 수 없습니다."),
    FILE_READ_ERROR(500, "FILE_002", "파일 읽기 오류입니다."),
    FILE_WRITE_ERROR(500, "FILE_003", "파일 쓰기 오류입니다."),
    UNSUPPORTED_FILE_FORMAT(400, "FILE_004", "지원되지 않는 파일 형식입니다."),
    FILE_TOO_LARGE(400, "FILE_005", "파일 크기가 너무 큽니다."),
    FILE_EMPTY(400, "FILE_006", "파일이 비어있습니다."),

    // ========================================
    // 서비스 통신 에러 (8xxx)
    // ========================================
    SERVICE_UNAVAILABLE(503, "SVC_001", "서비스를 사용할 수 없습니다."),
    SERVICE_TIMEOUT(504, "SVC_002", "서비스 응답 시간 초과입니다."),
    SERVICE_COMMUNICATION_ERROR(500, "SVC_003", "서비스 간 통신 오류입니다."),
    ANALYZER_SERVICE_ERROR(500, "SVC_004", "Analyzer 서비스 오류입니다."),
    GENERATOR_SERVICE_ERROR(500, "SVC_005", "Generator 서비스 오류입니다.");

    private final int status;
    private final String code;
    private final String message;
}
