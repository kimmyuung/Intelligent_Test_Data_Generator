package com.itdg.generator.strategy.impl;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Component
public class NumericGeneratorStrategy implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("INT") || type.contains("NUMBER") || type.contains("DECIMAL")
                || type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("REAL")
                || type.contains("NUMERIC");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        String type = column.getDataType().toUpperCase();
        if (type.contains("INT") || type.contains("SERIAL")) { // INTEGER, BIGINT, SMALLINT, SERIAL
            // 기본적으로 0 ~ 10000 사이의 값
            return random.nextInt(10000);
        } else if (type.contains("DOUBLE") || type.contains("FLOAT")) {
            return random.nextDouble() * 1000;
        } else { // DECIMAL, NUMERIC
            return new BigDecimal(random.nextInt(10000));
        }
    }
}
