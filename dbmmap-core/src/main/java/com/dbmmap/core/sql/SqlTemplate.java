package com.dbmmap.core.sql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlTemplate {

    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern KEY_PATTERN = Pattern.compile("\\.");

    private static final Map<Long, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    public static BoundSql process(String template, Map<String, Object> params) {
        List<Object> paramList = new ArrayList<>();

        Matcher replaceMatcher = REPLACE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (replaceMatcher.find()) {
            String key = replaceMatcher.group(1);
            Object value = params.get(key);
            replaceMatcher.appendReplacement(sb, value != null ? value.toString() : "");
        }
        replaceMatcher.appendTail(sb);

        String sql = sb.toString();
        Matcher paramMatcher = PARAM_PATTERN.matcher(sql);
        sb = new StringBuffer();
        while (paramMatcher.find()) {
            String key = paramMatcher.group(1);
            Object value = getParamValue(params, key);
            paramList.add(value);
            paramMatcher.appendReplacement(sb, "?");
        }
        paramMatcher.appendTail(sb);

        return new BoundSql(sb.toString(), paramList.toArray());
    }

    private static Object getParamValue(Map<String, Object> params, String key) {
        String[] parts = KEY_PATTERN.split(key);
        Object value = params.get(parts[0]);
        if (value == null) {
            return null;
        }
        int length = parts.length;
        for (int i = 1; i < length; i++) {
            try {
                Field field = fetchField(value, parts[i]);
                value = field.get(value);
            } catch (Exception e) {
                return null;
            }
        }
        return value;
    }

    private static Field fetchField(Object value, String parts) throws NoSuchFieldException {
        Class<?> clazz = value.getClass();
        long classHash = clazz.hashCode();
        long fieldNameHash = parts.hashCode();
        long key = (classHash << 32) | fieldNameHash;
        Field field = FIELD_CACHE.get(key);
        if (field == null) {
            field = fetchField0(clazz, parts);
            FIELD_CACHE.put(key, field);
        }
        return field;
    }

    private static Field fetchField0(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
