package com.dbmmap.core.type;

import com.dbmmap.core.type.handlers.BooleanTypeHandler;
import com.dbmmap.core.type.handlers.DateTypeHandler;
import com.dbmmap.core.type.handlers.BigDecimalTypeHandler;
import com.dbmmap.core.type.handlers.IntegerTypeHandler;
import com.dbmmap.core.type.handlers.LongTypeHandler;
import com.dbmmap.core.type.handlers.StringTypeHandler;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for TypeHandler instances.
 * Supports registration and lookup by Java type or SQL type.
 * Built-in handlers are registered by default.
 */
public class TypeHandlerRegistry {

    private final Map<Class<?>, TypeHandler<?>> javaTypeHandlerMap = new ConcurrentHashMap<>();
    private final Map<Integer, TypeHandler<?>> sqlTypeHandlerMap = new ConcurrentHashMap<>();

    /**
     * Constructs a TypeHandlerRegistry with built-in handlers registered.
     */
    public TypeHandlerRegistry() {
        registerBuiltInHandlers();
    }

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
     * Registers a TypeHandler for a Java type.
     *
     * @param <T> the Java type
     * @param javaType the Java class
     * @param handler the TypeHandler instance
     */
    public <T> void register(Class<T> javaType, TypeHandler<? extends T> handler) {
        javaTypeHandlerMap.put(javaType, handler);
    }

    /**
     * Registers a TypeHandler for an SQL type.
     *
     * @param sqlType the SQL type (from java.sql.Types)
     * @param handler the TypeHandler instance
     */
    public void register(int sqlType, TypeHandler<?> handler) {
        sqlTypeHandlerMap.put(sqlType, handler);
    }

    /**
     * Gets a TypeHandler by Java type.
     *
     * @param <T> the Java type
     * @param javaType the Java class
     * @return the TypeHandler, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getTypeHandler(Class<T> javaType) {
        return (TypeHandler<T>) javaTypeHandlerMap.get(javaType);
    }

    /**
     * Gets a TypeHandler by SQL type.
     *
     * @param sqlType the SQL type (from java.sql.Types)
     * @return the TypeHandler, or null if not registered
     */
    public TypeHandler<?> getTypeHandler(int sqlType) {
        return sqlTypeHandlerMap.get(sqlType);
    }

    /**
     * Checks if a TypeHandler is registered for a Java type.
     *
     * @param javaType the Java class
     * @return true if a handler is registered
     */
    public boolean hasTypeHandler(Class<?> javaType) {
        return javaTypeHandlerMap.containsKey(javaType);
    }

    /**
     * Checks if a TypeHandler is registered for an SQL type.
     *
     * @param sqlType the SQL type (from java.sql.Types)
     * @return true if a handler is registered
     */
    public boolean hasTypeHandler(int sqlType) {
        return sqlTypeHandlerMap.containsKey(sqlType);
    }
}