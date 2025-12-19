package com.itdg.orchestrator.exception;

/**
 * 서비스 통신 관련 예외
 */
public class ServiceCommunicationException extends BusinessException {

    private final String serviceName;

    public ServiceCommunicationException(ErrorCode errorCode, String serviceName) {
        super(errorCode, serviceName);
        this.serviceName = serviceName;
    }

    public ServiceCommunicationException(ErrorCode errorCode, String serviceName, Throwable cause) {
        super(errorCode, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    // 팩토리 메서드
    public static ServiceCommunicationException analyzerError(Throwable cause) {
        return new ServiceCommunicationException(ErrorCode.ANALYZER_SERVICE_ERROR, "Analyzer", cause);
    }

    public static ServiceCommunicationException generatorError(Throwable cause) {
        return new ServiceCommunicationException(ErrorCode.GENERATOR_SERVICE_ERROR, "Generator", cause);
    }

    public static ServiceCommunicationException timeout(String serviceName) {
        return new ServiceCommunicationException(ErrorCode.SERVICE_TIMEOUT, serviceName);
    }

    public static ServiceCommunicationException unavailable(String serviceName) {
        return new ServiceCommunicationException(ErrorCode.SERVICE_UNAVAILABLE, serviceName);
    }
}
