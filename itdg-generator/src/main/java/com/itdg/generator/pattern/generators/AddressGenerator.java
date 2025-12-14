package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AddressGenerator implements DataGeneratorStrategy {
    private static final String[] CITIES = { "서울시", "부산시", "대구시", "인천시", "광주시", "대전시", "울산시", "경기도", "강원도" };
    private static final String[] DISTRICTS = { "강남구", "강서구", "서초구", "송파구", "영등포구", "마포구", "종로구", "중구", "동대문구", "분당구" };
    private static final String[] ROADS = { "테헤란로", "강남대로", "도산대로", "대학로", "세종대로", "올림픽로", "가로수길" };

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getName() == null)
            return false;
        String name = column.getName().toLowerCase();
        return name.contains("addr") || name.contains("location") || name.contains("place");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        return CITIES[random.nextInt(CITIES.length)] + " " +
                DISTRICTS[random.nextInt(DISTRICTS.length)] + " " +
                ROADS[random.nextInt(ROADS.length)] + " " +
                (random.nextInt(100) + 1) + "길 " + (random.nextInt(50) + 1);
    }
}
