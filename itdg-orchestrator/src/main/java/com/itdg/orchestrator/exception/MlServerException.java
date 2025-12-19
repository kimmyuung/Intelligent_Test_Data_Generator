package com.itdg.orchestrator.exception;

/**
 * ML 서버 관련 예외
 */
public class MlServerException extends BusinessException {

    public MlServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MlServerException(ErrorCode errorCode, String additionalMessage) {
        super(errorCode, additionalMessage);
    }

    public MlServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    // 팩토리 메서드
    public static MlServerException unavailable() {
        return new MlServerException(ErrorCode.ML_SERVER_UNAVAILABLE);
    }

    public static MlServerException modelNotFound(String modelId) {
        return new MlServerException(ErrorCode.ML_MODEL_NOT_FOUND, "Model ID: " + modelId);
    }

    public static MlServerException trainingFailed(String reason) {
        return new MlServerException(ErrorCode.ML_TRAINING_FAILED, reason);
    }

    public static MlServerException generationFailed(String reason) {
        return new MlServerException(ErrorCode.ML_GENERATION_FAILED, reason);
    }

    public static MlServerException fileAnalysisFailed(String reason) {
        return new MlServerException(ErrorCode.ML_FILE_ANALYSIS_FAILED, reason);
    }
}
