package com.itdg.common.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadata {
    private String name;
    private String dataType;
    private Integer length;
    private Boolean isPrimaryKey;
    private Boolean isNullable;
    private Boolean isAutoIncrement;
    private Boolean isForeignKey;
    private String foreignKeyTargetTable;
    private String comment;
}
