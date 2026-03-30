package com.dbmmap.core.type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for TypeHandler instances.
 * Supports registration and lookup by Java type or SQL type.
 */
public class TypeHandlerRegistry {

    private final Map<Class<?>, TypeHandler<?>> javaTypeHandlerMap = new ConcurrentHashMap<>();
    private final Map<Integer, TypeHandler<?>> sqlTypeHandlerMap = new ConcurrentHashMap<>();

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