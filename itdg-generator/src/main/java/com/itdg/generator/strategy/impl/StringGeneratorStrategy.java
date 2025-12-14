package com.itdg.generator.strategy.impl;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class StringGeneratorStrategy implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getDataType() == null)
            return false;
        String type = column.getDataType().toUpperCase();
        return type.contains("CHAR") || type.contains("TEXT") || type.contains("STRING") || type.contains("VARCHAR");
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        // 길이에 맞는 랜덤 문자열 생성
        int length = column.getLength() != null && column.getLength() > 0 ? column.getLength() : 20;
        if (length > 100)
            length = 100; // 너무 긴 문자열 방지 (기본값)

        // 간단한 랜덤 문자열
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, Math.min(length, uuid.length()));
    }
}
