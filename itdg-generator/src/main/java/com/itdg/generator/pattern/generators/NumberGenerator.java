package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class NumberGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("INT") || type.contains("FLOAT") || type.contains("DOUBLE")
                || type.contains("DECIMAL") || type.contains("NUMERIC") || type.contains("NUMBER")
                || type.contains("LONG") || type.contains("BIGINT");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        String type = column.getDataType().toUpperCase();

        // Simple heuristic for range. TODO: Check constraints if available.
        if (type.contains("BIGINT") || type.contains("LONG")) {
            return random.nextLong(1000000);
        } else if (type.contains("FLOAT") || type.contains("DOUBLE") || type.contains("DECIMAL")
                || type.contains("NUMERIC")) {
            double val = random.nextDouble() * 10000;
            return String.format("%.2f", val); // Return as string or double depending on needs? JSON handles numbers.
            // Let's return Double for JSON compatibility
            // return Math.round(val * 100.0) / 100.0;
        } else {
            // Integer default
            return random.nextInt(10000);
        }
    }
}
