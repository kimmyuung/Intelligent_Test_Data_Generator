package com.itdg.generator.strategy;

import com.itdg.common.dto.metadata.ColumnMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class StatisticalStrategy implements DataGeneratorStrategy {

    @Override
    public boolean supports(ColumnMetadata column) {
        String name = column.getName().toLowerCase();
        String type = column.getDataType().toUpperCase();

        // Support numerical columns that look like statistics (age, score, price, etc.)
        // User Request: Skip Auto-Increment columns (they don't need learning)
        if (Boolean.TRUE.equals(column.getIsAutoIncrement())) {
            return false;
        }

        // User Request: Skip PII columns (protect privacy, use rule-based generators
        // instead)
        if (name.contains("phone") || name.contains("mobile") || name.contains("tel") ||
                name.contains("email") || name.contains("mail") ||
                name.contains("password") || name.contains("pwd") ||
                name.contains("ssn") || name.contains("resident")) {
            return false;
        }

        boolean isNumeric = type.contains("INT") || type.contains("DOUBLE") || type.contains("FLOAT")
                || type.contains("NUMBER");
        boolean isStatField = name.contains("age") || name.contains("score") || name.contains("grade")
                || name.contains("price") || name.contains("level");

        return isNumeric && isStatField;
    }

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        String name = column.getName().toLowerCase();

        if (name.contains("age")) {
            // Normal distribution: Mean=35, SD=15, Min=18, Max=90
            return (int) generateGaussian(random, 35, 15, 18, 90);
        }
        if (name.contains("score") || name.contains("grade")) {
            // Normal distribution: Mean=75, SD=10, Min=0, Max=100
            return (int) generateGaussian(random, 75, 10, 0, 100);
        }
        if (name.contains("level")) {
            // Skewed low: Mean=10, SD=20, Min=1, Max=99
            return (int) generateGaussian(random, 10, 20, 1, 99);
        }
        if (name.contains("price") || name.contains("cost")) {
            // Mean=10000, SD=5000, Min=1000
            return (int) generateGaussian(random, 10000, 5000, 1000, 1000000);
        }

        return random.nextInt(100);
    }

    private double generateGaussian(Random random, double mean, double stdDev, double min, double max) {
        double value;
        do {
            value = (random.nextGaussian() * stdDev) + mean;
        } while (value < min || value > max);
        return value;
    }
}
