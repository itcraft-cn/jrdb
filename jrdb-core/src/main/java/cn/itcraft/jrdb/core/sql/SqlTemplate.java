package cn.itcraft.jrdb.core.sql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL模板处理类
 * 
 * <p>用于处理带有占位符的SQL模板，支持两种占位符类型：
 * <ul>
 *     <li>{@code #{param}} - 参数占位符，会被替换为{@code ?}，参数值存入参数列表</li>
 *     <li>{@code ${param}} - 直接替换占位符，会被替换为参数的实际值</li>
 * </ul>
 * 
 * <p>支持嵌套属性访问，例如{@code #{user.address.city}}会依次获取user对象的address属性，再获取address的city属性。
 * 
 * <p>使用反射获取字段值，并对字段进行缓存以提升性能。
 * 
 * @author jrdb
 * @see BoundSql
 */
public class SqlTemplate {

    /**
     * 参数占位符正则表达式
     * 
     * <p>匹配{@code #{param}}形式的占位符，其中param为参数名（可包含嵌套属性路径）。
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    /**
     * 直接替换占位符正则表达式
     * 
     * <p>匹配{@code ${param}}形式的占位符，其中param为参数名。
     */
    private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 属性分隔符正则表达式
     * 
     * <p>用于分割嵌套属性路径，例如{@code user.address.city}会被分割为["user", "address", "city"]。
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("\\.");

    /**
     * 字段缓存
     * 
     * <p>缓存已获取的字段对象，避免重复反射操作。
     * key由类哈希码和字段名哈希码组合而成，value为对应的字段对象。
     */
    private static final Map<Long, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 处理SQL模板
     * 
     * <p>处理流程：
     * <ol>
     *     <li>先处理{@code ${param}}占位符，将其替换为参数的实际值（字符串形式）</li>
     *     <li>再处理{@code #{param}}占位符，将其替换为{@code ?}，并将参数值收集到列表中</li>
     * </ol>
     * 
     * @param template SQL模板字符串，可包含{@code #{param}}和{@code ${param}}占位符
     * @param params 参数映射表，key为参数名，value为参数值
     * @return 处理后的SQL绑定对象，包含处理后的SQL语句和参数列表
     */
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

    /**
     * 获取参数值
     * 
     * <p>支持嵌套属性访问。如果参数名包含点号（如{@code user.address.city}），
     * 会依次获取各级属性值。
     * 
     * @param params 参数映射表
     * @param key 参数名，可以是简单名称或嵌套属性路径
     * @return 参数值，如果参数不存在或嵌套属性访问失败则返回null
     */
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

    /**
     * 获取字段对象（带缓存）
     * 
     * <p>首先从缓存中查找字段，如果不存在则通过反射获取并缓存。
     * 缓存的key由类哈希码和字段名哈希码组合而成。
     * 
     * @param value 包含字段的对象
     * @param parts 字段名
     * @return 字段对象
     * @throws NoSuchFieldException 如果字段不存在
     */
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

    /**
     * 通过反射获取字段对象
     * 
     * <p>获取指定类中声明的字段，并设置其为可访问状态。
     * 
     * @param clazz 目标类
     * @param name 字段名
     * @return 字段对象
     * @throws RuntimeException 如果字段不存在，将{@link NoSuchFieldException}包装为运行时异常抛出
     */
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