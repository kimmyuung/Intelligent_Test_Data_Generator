package com.itdg.orchestrator.exception;

/**
 * 데이터베이스 연결 예외
 */
public class DatabaseConnectionException extends BusinessException {

    public DatabaseConnectionException() {
        super(ErrorCode.DATABASE_CONNECTION_FAILED);
    }

    public DatabaseConnectionException(String additionalMessage) {
        super(ErrorCode.DATABASE_CONNECTION_FAILED, additionalMessage);
    }

    public DatabaseConnectionException(Throwable cause) {
        super(ErrorCode.DATABASE_CONNECTION_FAILED, cause);
    }
}
