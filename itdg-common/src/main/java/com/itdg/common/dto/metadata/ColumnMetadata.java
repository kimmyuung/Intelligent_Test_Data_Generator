package com.itdg.common.dto.metadata;

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
public class ColumnMetadata {
    private String name;
    private String dataType;
    private Integer length;
    private boolean isPrimaryKey;
    private boolean isNullable;
    private boolean isAutoIncrement;
    private String comment;
}
