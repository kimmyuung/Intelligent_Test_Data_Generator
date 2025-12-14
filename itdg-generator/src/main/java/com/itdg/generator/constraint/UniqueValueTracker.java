package com.itdg.generator.constraint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks unique values for columns to enforce UNIQUE constraints.
 * This class is stateful and should be instantiated per generation
 * request/table.
 */
public class UniqueValueTracker {
    private final Map<String, Set<Object>> uniqueValues = new HashMap<>();

    public boolean isUnique(String columnName, Object value) {
        Set<Object> values = uniqueValues.computeIfAbsent(columnName, k -> new HashSet<>());
        if (values.contains(value)) {
            return false;
        }
        values.add(value); // Add immediately (assume it will be used)
        return true;
    }

    public void add(String columnName, Object value) {
        uniqueValues.computeIfAbsent(columnName, k -> new HashSet<>()).add(value);
    }

    public void reset(String columnName) {
        uniqueValues.remove(columnName);
    }
}
