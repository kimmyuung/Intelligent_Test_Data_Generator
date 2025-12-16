package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100) // Lower priority than specific patterns
public class DateGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        String type = column.getDataType().toUpperCase();

        // Generate random date within last 10 years to +1 year
        int daysToSubtract = random.nextInt(365 * 10);
        int daysToAdd = random.nextInt(365);

        LocalDate date = LocalDate.now().minusDays(daysToSubtract).plusDays(daysToAdd);
        LocalTime time = LocalTime.of(random.nextInt(24), random.nextInt(60), random.nextInt(60));

        if (type.contains("TIMESTAMP") || type.contains("DATETIME")) {
            return LocalDateTime.of(date, time).toString();
        } else if (type.equals("TIME")) {
            return time.toString();
        } else {
            // Default to DATE
            return date.toString();
        }
    }
}
