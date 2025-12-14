package com.itdg.generator.strategy;

import com.itdg.common.dto.metadata.ColumnMetadata;
import java.util.Random;

public interface DataGeneratorStrategy {
    Object generate(ColumnMetadata column, Random random);

    boolean supports(ColumnMetadata column);
}
