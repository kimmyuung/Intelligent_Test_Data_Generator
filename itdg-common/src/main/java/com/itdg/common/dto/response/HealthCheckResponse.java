package com.itdg.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    private String serviceName;

    private String status;

    private String version;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String message;
}