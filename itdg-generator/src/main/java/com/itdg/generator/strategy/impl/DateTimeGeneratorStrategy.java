package com.itdg.generator.strategy.impl;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

@Component
public class DateTimeGeneratorStrategy implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("DATE") || type.contains("TIME");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        String type = column.getDataType().toUpperCase();

        // 최근 1년 ~ 미래 1년 사이 랜덤 날짜
        long minDay = LocalDate.now().minusYears(1).toEpochDay();
        long maxDay = LocalDate.now().plusYears(1).toEpochDay();
        long randomDay = minDay + random.nextInt((int) (maxDay - minDay));

        LocalDate randomDate = LocalDate.ofEpochDay(randomDay);

        if (type.equals("DATE")) {
            return randomDate;
        } else if (type.contains("TIME") && !type.contains("STAMP")) { // TIME only
            return LocalTime.of(random.nextInt(24), random.nextInt(60));
        } else { // TIMESTAMP, DATETIME
            return LocalDateTime.of(randomDate, LocalTime.of(random.nextInt(24), random.nextInt(60)));
        }
    }
}
