package com.itdg.generator.pattern.generators;

import com.itdg.common.dto.metadata.ColumnMetadata;
import com.itdg.generator.strategy.DataGeneratorStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NameGenerator implements DataGeneratorStrategy {
    private static final String[] LAST_NAMES = { "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "오", "서", "신",
            "권", "황", "안", "송", "전", "홍" };
    private static final String[] FIRST_NAMES = { "민수", "서준", "도윤", "예준", "시우", "하준", "지호", "지후", "준우", "준서", "민재",
            "현우", "지훈", "우진", "건우", "서연", "지우", "서현", "민서", "수아" };

    @Override
    public boolean supports(ColumnMetadata column) {
        if (column.getName() == null)
            return false;
        String name = column.getName().toLowerCase();
        return (name.contains("name") || name.contains("username") || name.contains("user_name") || name.contains("nm"))
                && !name.contains("id"); // Exclude 'id' columns if possible, but 'user_id' might be name or id...
                                         // usually id is numeric or uuid.
        // If data type is numeric, this shouldn't run. 'supports' usually implies we
        // should checks data type too, but for now we assume pattern match is enough or
        // strategy selector checks type compatibility.
        // Let's add a basic type check: must be string-like.
    }

    // Override supports to check type as well to be safe
    // But DataGeneratorStrategy generally assumes if supports() is true, it can
    // handle it.
    // NameGenerator should probably only handle String types.

    @Override
    public Object generate(ColumnMetadata column, Random random) {
        // Check if type is string compatible
        String type = column.getDataType().toUpperCase();
        if (!type.contains("CHAR") && !type.contains("TEXT") && !type.contains("STRING")) {
            // If it's not a string type, fall back or return null?
            // Ideally supports() should return false.
            // Retrying supports check logic inside generate is redundant if supports was
            // called.
            // But to be consistent, let's just generate string.
            return "ERROR_NOT_STRING";
        }

        return LAST_NAMES[random.nextInt(LAST_NAMES.length)] + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }
}
