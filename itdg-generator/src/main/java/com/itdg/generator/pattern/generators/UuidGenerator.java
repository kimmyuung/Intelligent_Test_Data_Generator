package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // High precedence if explicit UUID type or name match
public class UuidGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() != null
                && (column.getDataType().equalsIgnoreCase("UUID") || column.getDataType().contains("GUID"))) {
            return true;
        }
        // Also supports if column name sounds likd UUID but type might be string
        if (column.getName() != null) {
            String name = column.getName().toLowerCase();
            return name.endsWith("uuid") || name.endsWith("guid");
        }
        return false;
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        return UUID.randomUUID().toString();
    }
}
