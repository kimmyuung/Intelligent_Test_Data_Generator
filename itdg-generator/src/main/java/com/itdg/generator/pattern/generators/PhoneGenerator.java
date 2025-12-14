package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PhoneGenerator implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getName() == null)
            return false;
        String name = column.getName().toLowerCase();
        return name.contains("phone") || name.contains("tel") || name.contains("mobile") || name.contains("contact");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        // 010-xxxx-xxxx
        return "010-" + String.format("%04d", random.nextInt(10000)) + "-"
                + String.format("%04d", random.nextInt(10000));
    }
}
