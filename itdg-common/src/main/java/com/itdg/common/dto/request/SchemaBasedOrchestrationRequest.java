package com.itdg.common.dto.request;

import com.itdg.common.dto.metadata.TableMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SchemaBasedOrchestrationRequest {
    private List<TableMetadata> tables;
    private Long seed;
}
