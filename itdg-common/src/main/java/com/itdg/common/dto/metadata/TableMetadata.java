package com.itdg.common.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
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
}
