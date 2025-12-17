package com.itdg.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ML Server 응답 DTO들
 */
public class MlServerDto {

    /**
     * 모델 학습 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainResponse {
        private boolean success;
        private String modelId;
        private String status;
        private String modelType;
        private List<String> columns;
        private int rowCount;
        private double trainingTime;
    }

    /**
     * 합성 데이터 생성 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateResponse {
        private boolean success;
        private List<Map<String, Object>> data;
        private List<String> columns;
        private int rowCount;
    }

    /**
     * 모델 정보 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfoResponse {
        private boolean success;
        private String modelId;
        private boolean exists;
        private long size;
        private double createdAt;
        private double expiresIn;
    }

    /**
     * Health Check 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthResponse {
        private String status;
    }

    /**
     * 일반 성공/실패 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleResponse {
        private boolean success;
        private String message;
    }
}
