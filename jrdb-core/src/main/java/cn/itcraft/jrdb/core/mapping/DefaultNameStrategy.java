package cn.itcraft.jrdb.core.mapping;

import java.util.Objects;

/**
 * Default implementation of NameStrategy.
 * <p>
 * Converts between snake_case database column names and camelCase Java field names.
 * Handles edge cases including null, empty strings, consecutive underscores, and
 * leading/trailing underscores.
 * </p>
 *
 * @author jrdb
 * @since 1.0.0
 */
public class DefaultNameStrategy implements NameStrategy {

    private static final String DEFAULT_NAME = "default";
    private static final char UNDERSCORE = '_';

    @Override
    public String columnNameToFieldName(String columnName) {
        if (columnName == null) {
            return null;
        }
        if (columnName.isEmpty()) {
            return columnName;
        }

        StringBuilder result = new StringBuilder(columnName.length());
        boolean nextUpper = false;
        boolean isFirstChar = true;

        for (int i = 0; i < columnName.length(); i++) {
            char c = columnName.charAt(i);
            if (c == UNDERSCORE) {
                if (!isFirstChar) {
                    nextUpper = true;
                }
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
                isFirstChar = false;
            }
        }

        return result.toString();
    }

    @Override
    public String fieldNameToColumnName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        if (fieldName.isEmpty()) {
            return fieldName;
        }

        StringBuilder result = new StringBuilder(fieldName.length() * 2);

        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append(UNDERSCORE);
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DefaultNameStrategy{name='" + DEFAULT_NAME + "'}";
    }
}