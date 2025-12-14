package com.itdg.common.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SchemaMetadata {
    private String databaseName;

    @Builder.Default
    private List<TableMetadata> tables = new ArrayList<>();

    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();

    // 프로젝트 분석 시 메타 정보
    private ProjectInfo projectInfo;
}
