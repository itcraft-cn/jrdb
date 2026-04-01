package cn.itcraft.jrdb.jdbc.type;

import cn.itcraft.jrdb.jdbc.type.handlers.BooleanTypeHandler;
import cn.itcraft.jrdb.jdbc.type.handlers.DateTypeHandler;
import cn.itcraft.jrdb.jdbc.type.handlers.BigDecimalTypeHandler;
import cn.itcraft.jrdb.jdbc.type.handlers.IntegerTypeHandler;
import cn.itcraft.jrdb.jdbc.type.handlers.LongTypeHandler;
import cn.itcraft.jrdb.jdbc.type.handlers.StringTypeHandler;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型处理器注册中心。
 *
 * <p>该类负责管理所有 {@link TypeHandler} 实例的注册与查找，
 * 支持通过 Java 类型或 SQL 类型进行类型处理器的查找。
 * 内置处理器在构造时自动注册。</p>
 *
 * <p>内置支持的类型包括：</p>
 * <ul>
 *   <li>Integer / int - 整型</li>
 *   <li>Long / long - 长整型</li>
 *   <li>String - 字符串</li>
 *   <li>Boolean / boolean - 布尔型</li>
 *   <li>Date - 日期时间</li>
 *   <li>BigDecimal - 高精度数值</li>
 * </ul>
 *
 * <p>该类使用 {@link ConcurrentHashMap} 作为底层存储，保证线程安全。</p>
 *
 * @author jrdb
 * @see TypeHandler
 * @see TypeHandler#setParameter(java.sql.PreparedStatement, int, Object)
 * @see TypeHandler#getResult(java.sql.ResultSet, int)
 */
public class TypeHandlerRegistry {

    /**
     * Java 类型到类型处理器的映射表。
     *
     * <p>键为 Java 类型（如 {@code Integer.class}、{@code String.class}），
     * 值为对应的 {@link TypeHandler} 实例。</p>
     */
    private final Map<Class<?>, TypeHandler<?>> javaTypeHandlerMap = new ConcurrentHashMap<>();

    /**
     * SQL 类型到类型处理器的映射表。
     *
     * <p>键为 SQL 类型常量（来自 {@link java.sql.Types}，如 {@code Types.INTEGER}、
     * {@code Types.VARCHAR}），值为对应的 {@link TypeHandler} 实例。</p>
     */
    private final Map<Integer, TypeHandler<?>> sqlTypeHandlerMap = new ConcurrentHashMap<>();

    /**
     * 构造一个类型处理器注册中心，并自动注册内置处理器。
     *
     * <p>构造时会调用 {@link #registerBuiltInHandlers()} 方法注册所有内置类型处理器，
     * 包括整型、长整型、字符串、布尔型、日期和高精度数值等常见类型。</p>
     */
    public TypeHandlerRegistry() {
        registerBuiltInHandlers();
    }

    /**
     * 注册所有内置的类型处理器。
     *
     * <p>该方法在构造函数中被调用，注册以下类型的处理器：</p>
     * <ul>
     *   <li>{@link Integer} 和 {@code int} 类型，映射到 {@link Types#INTEGER}</li>
     *   <li>{@link Long} 和 {@code long} 类型，映射到 {@link Types#BIGINT}</li>
     *   <li>{@link String} 类型，映射到 {@link Types#VARCHAR}、{@link Types#CHAR}、{@link Types#LONGVARCHAR}</li>
     *   <li>{@link Boolean} 和 {@code boolean} 类型，映射到 {@link Types#BOOLEAN}、{@link Types#BIT}</li>
     *   <li>{@link Date} 类型，映射到 {@link Types#TIMESTAMP}、{@link Types#DATE}、{@link Types#TIME}</li>
     *   <li>{@link BigDecimal} 类型，映射到 {@link Types#DECIMAL}、{@link Types#NUMERIC}</li>
     * </ul>
     */
    private void registerBuiltInHandlers() {
        register(Integer.class, new IntegerTypeHandler());
        register(Types.INTEGER, new IntegerTypeHandler());
        register(int.class, new IntegerTypeHandler());

        register(Long.class, new LongTypeHandler());
        register(Types.BIGINT, new LongTypeHandler());
        register(long.class, new LongTypeHandler());

        register(String.class, new StringTypeHandler());
        register(Types.VARCHAR, new StringTypeHandler());
        register(Types.CHAR, new StringTypeHandler());
        register(Types.LONGVARCHAR, new StringTypeHandler());

        register(Boolean.class, new BooleanTypeHandler());
        register(Types.BOOLEAN, new BooleanTypeHandler());
        register(Types.BIT, new BooleanTypeHandler());
        register(boolean.class, new BooleanTypeHandler());

        register(Date.class, new DateTypeHandler());
        register(Types.TIMESTAMP, new DateTypeHandler());
        register(Types.DATE, new DateTypeHandler());
        register(Types.TIME, new DateTypeHandler());

        register(BigDecimal.class, new BigDecimalTypeHandler());
        register(Types.DECIMAL, new BigDecimalTypeHandler());
        register(Types.NUMERIC, new BigDecimalTypeHandler());
    }

    /**
     * 为指定的 Java 类型注册类型处理器。
     *
     * <p>如果该 Java 类型已有关联的处理器，则新处理器将覆盖原有处理器。</p>
     *
     * @param <T> Java 类型的泛型参数
     * @param javaType Java 类型的 Class 对象
     * @param handler 要注册的类型处理器实例
     */
    public <T> void register(Class<T> javaType, TypeHandler<? extends T> handler) {
        javaTypeHandlerMap.put(javaType, handler);
    }

    /**
     * 为指定的 SQL 类型注册类型处理器。
     *
     * <p>如果该 SQL 类型已有关联的处理器，则新处理器将覆盖原有处理器。
     * SQL 类型常量来自 {@link java.sql.Types}。</p>
     *
     * @param sqlType SQL 类型常量（来自 {@link java.sql.Types}）
     * @param handler 要注册的类型处理器实例
     */
    public void register(int sqlType, TypeHandler<?> handler) {
        sqlTypeHandlerMap.put(sqlType, handler);
    }

    /**
     * 根据 Java 类型获取对应的类型处理器。
     *
     * <p>从 Java 类型映射表中查找已注册的类型处理器。
     * 支持包装类型和基本类型，如 {@code Integer.class} 和 {@code int.class}。</p>
     *
     * @param <T> Java 类型的泛型参数
     * @param javaType Java 类型的 Class 对象
     * @return 对应的类型处理器，如果未注册则返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getTypeHandler(Class<T> javaType) {
        return (TypeHandler<T>) javaTypeHandlerMap.get(javaType);
    }

    /**
     * 根据 SQL 类型获取对应的类型处理器。
     *
     * <p>从 SQL 类型映射表中查找已注册的类型处理器。
     * SQL 类型常量来自 {@link java.sql.Types}。</p>
     *
     * @param sqlType SQL 类型常量（来自 {@link java.sql.Types}）
     * @return 对应的类型处理器，如果未注册则返回 {@code null}
     */
    public TypeHandler<?> getTypeHandler(int sqlType) {
        return sqlTypeHandlerMap.get(sqlType);
    }

    /**
     * 检查指定的 Java 类型是否已注册类型处理器。
     *
     * @param javaType Java 类型的 Class 对象
     * @return 如果已注册返回 {@code true}，否则返回 {@code false}
     */
    public boolean hasTypeHandler(Class<?> javaType) {
        return javaTypeHandlerMap.containsKey(javaType);
    }

    /**
     * 检查指定的 SQL 类型是否已注册类型处理器。
     *
     * <p>SQL 类型常量来自 {@link java.sql.Types}。</p>
     *
     * @param sqlType SQL 类型常量（来自 {@link java.sql.Types}）
     * @return 如果已注册返回 {@code true}，否则返回 {@code false}
     */
    public boolean hasTypeHandler(int sqlType) {
        return sqlTypeHandlerMap.containsKey(sqlType);
    }
}