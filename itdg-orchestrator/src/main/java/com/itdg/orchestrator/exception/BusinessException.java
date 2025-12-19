package com.itdg.orchestrator.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 * 
 * HTTP 상태 코드와 에러 코드를 포함하여 세분화된 예외 처리 지원
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + ": " + additionalMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return errorCode.getStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
