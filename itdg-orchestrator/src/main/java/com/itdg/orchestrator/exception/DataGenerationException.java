package com.itdg.orchestrator.exception;

/**
 * 데이터 생성 관련 예외
 */
public class DataGenerationException extends BusinessException {

    public DataGenerationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DataGenerationException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode, additionalMessage);
    }

    public DataGenerationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    // 팩토리 메서드
    public static DataGenerationException generationFailed(String reason) {
        return new DataGenerationException(ErrorCode.DATA_GENERATION_FAILED, reason);
    }

    public static DataGenerationException invalidRowCount(int rowCount) {
        return new DataGenerationException(ErrorCode.INVALID_ROW_COUNT, "요청된 행 수: " + rowCount);
    }

    public static DataGenerationException noTablesSelected() {
        return new DataGenerationException(ErrorCode.NO_TABLES_SELECTED);
    }
}
