package com.itdg.generator.strategy.impl;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class BooleanGeneratorStrategy implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("BOOL") || type.contains("BIT");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        return random.nextBoolean();
    }
}
