package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmailGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getName() == null)
            return false;
        String name = column.getName().toLowerCase();
        return name.contains("email") || name.contains("mail");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        return "user" + random.nextInt(10000) + "@example.com";
    }
}
