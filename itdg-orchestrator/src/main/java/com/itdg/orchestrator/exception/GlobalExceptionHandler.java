package com.itdg.orchestrator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 전역 예외 처리기
 * 
 * 모든 예외를 일관된 형식으로 처리하고 적절한 HTTP 응답 반환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================================
    // 1. 비즈니스 예외 처리
    // ========================================

    /**
     * BusinessException 및 하위 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.error("Business exception occurred: [{}] {} - {}",
                ex.getCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(ex, request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * 데이터베이스 연결 예외 처리
     */
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseConnectionException(
            DatabaseConnectionException ex,
            HttpServletRequest request) {

        log.error("Database connection error: {} - {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.of(ex, request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * ML 서버 예외 처리
     */
    @ExceptionHandler(MlServerException.class)
    public ResponseEntity<ErrorResponse> handleMlServerException(
            MlServerException ex,
            HttpServletRequest request) {

        log.error("ML Server error: [{}] {} - {}",
                ex.getCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(ex, request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // ========================================
    // 2. 유효성 검증 예외 처리
    // ========================================

    /**
     * @Valid 유효성 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.warn("Validation failed: {} - {}", errors, request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_INPUT.getCode())
                .message("입력 값 검증에 실패했습니다.")
                .path(request.getRequestURI())
                .details(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * BindException 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Binding failed: {} - {}", errors, request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ErrorCode.INVALID_INPUT.getCode())
                .message("요청 데이터 바인딩에 실패했습니다.")
                .path(request.getRequestURI())
                .details(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // ========================================
    // 3. 외부 서비스 통신 예외 처리
    // ========================================

    /**
     * WebClient 응답 에러 처리
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponseException(
            WebClientResponseException ex,
            HttpServletRequest request) {

        log.error("External service error: {} {} - {}",
                ex.getStatusCode(), ex.getStatusText(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .status(ex.getStatusCode().value())
                .code(ErrorCode.SERVICE_COMMUNICATION_ERROR.getCode())
                .message("외부 서비스 통신 오류: " + ex.getStatusText())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * WebClient 요청 에러 (연결 실패 등) 처리
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> handleWebClientRequestException(
            WebClientRequestException ex,
            HttpServletRequest request) {

        log.error("External service request failed: {} - {}",
                ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.SERVICE_UNAVAILABLE,
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 연결 예외 처리
     */
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(
            ConnectException ex,
            HttpServletRequest request) {

        log.error("Connection failed: {} - {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.SERVICE_UNAVAILABLE,
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 타임아웃 예외 처리
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            TimeoutException ex,
            HttpServletRequest request) {

        log.error("Request timeout: {} - {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.SERVICE_TIMEOUT,
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    // ========================================
    // 4. 데이터베이스 예외 처리
    // ========================================

    /**
     * SQL 예외 처리
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(
            SQLException ex,
            HttpServletRequest request) {

        log.error("SQL error [{}]: {} - {}",
                ex.getSQLState(), ex.getMessage(), request.getRequestURI());

        // SQL 상태 코드에 따른 세분화
        ErrorCode errorCode;
        if (ex.getSQLState() != null && ex.getSQLState().startsWith("08")) {
            errorCode = ErrorCode.DATABASE_CONNECTION_FAILED;
        } else if (ex.getSQLState() != null && ex.getSQLState().startsWith("28")) {
            errorCode = ErrorCode.DATABASE_AUTHENTICATION_FAILED;
        } else {
            errorCode = ErrorCode.DATABASE_CONNECTION_FAILED;
        }

        ErrorResponse response = ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .details(Map.of("sqlState", ex.getSQLState() != null ? ex.getSQLState() : "N/A"))
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // ========================================
    // 5. 기타 예외 (Fallback)
    // ========================================

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Invalid argument: {} - {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_INPUT.getCode(),
                ex.getMessage(),
                request.getRequestURI());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 모든 처리되지 않은 예외 (Fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception: {} - {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
