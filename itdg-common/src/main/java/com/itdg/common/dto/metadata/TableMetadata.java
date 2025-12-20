package com.itdg.common.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    private String tableName;

    @Builder.Default
    private List<ColumnMetadata> columns = new ArrayList<>();

    @Builder.Default
    private List<String> primaryKeys = new ArrayList<>();

    private Long rowCount;
    private String comment;

    // ML 통합 필드
    private String mlModelId; // 학습된 ML 모델 ID (SDV)
    private Integer targetRowCount; // 생성할 행 수 (프론트엔드에서 지정)
}
