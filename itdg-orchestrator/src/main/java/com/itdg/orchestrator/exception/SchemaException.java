package com.itdg.orchestrator.exception;

/**
 * 스키마 분석 관련 예외
 */
public class SchemaException extends BusinessException {

    public SchemaException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SchemaException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode, additionalMessage);
    }

    public SchemaException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    // 팩토리 메서드
    public static SchemaException extractionFailed(String reason) {
        return new SchemaException(ErrorCode.SCHEMA_EXTRACTION_FAILED, reason);
    }

    public static SchemaException emptySchema() {
        return new SchemaException(ErrorCode.SCHEMA_EMPTY);
    }

    public static SchemaException unsupportedProjectType(String projectType) {
        return new SchemaException(ErrorCode.UNSUPPORTED_PROJECT_TYPE, projectType);
    }

    public static SchemaException tableNotFound(String tableName) {
        return new SchemaException(ErrorCode.TABLE_NOT_FOUND, tableName);
    }
}
