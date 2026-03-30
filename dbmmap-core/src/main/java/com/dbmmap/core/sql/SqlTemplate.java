package com.dbmmap.core.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlTemplate {
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
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
        String[] parts = key.split("\\.");
        Object value = params.get(parts[0]);
        for (int i = 1; i < parts.length && value != null; i++) {
            try {
                java.lang.reflect.Field field = value.getClass().getDeclaredField(parts[i]);
                field.setAccessible(true);
                value = field.get(value);
            } catch (Exception e) {
                return null;
            }
        }
        return value;
    }
}