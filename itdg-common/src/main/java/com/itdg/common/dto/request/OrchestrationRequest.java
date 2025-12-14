package com.itdg.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrchestrationRequest {
    private DbConnectionRequest dbConnection;
    private Integer rowCount;
    private Long seed;
}
