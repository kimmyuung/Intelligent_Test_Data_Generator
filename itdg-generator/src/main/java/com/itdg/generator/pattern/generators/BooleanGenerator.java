package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class BooleanGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("BOOL") || type.equals("BIT");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        return random.nextBoolean();
    }
}
