package com.itdg.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDataResponse {
    private Map<String, List<Map<String, Object>>> generatedData;
    private Map<String, Integer> statistics;
    private LocalDateTime generatedAt;
    private Long seed;
    private boolean success;
    private String message;
}
