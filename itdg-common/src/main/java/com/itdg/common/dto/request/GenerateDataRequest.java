package com.itdg.common.dto.request;

import com.itdg.common.dto.metadata.SchemaMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDataRequest {
    private SchemaMetadata schema;
    private Integer rowCount;
    private Long seed;
    private String outputFormat; // JSON, SQL, CSV
    private Map<String, Object> customRules;
}
