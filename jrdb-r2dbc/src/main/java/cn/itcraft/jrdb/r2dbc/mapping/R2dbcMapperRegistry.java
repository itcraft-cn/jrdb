package cn.itcraft.jrdb.r2dbc.mapping;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class R2dbcMapperRegistry {

    private final R2dbcMapperClassLoader classLoader;
    private final R2dbcMapperClassGenerator generator;

    private final Map<Class<?>, R2dbcEntityMapper<?>> mapperCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, R2dbcMappingConfig> configCache = new ConcurrentHashMap<>();

    public R2dbcMapperRegistry() {
        this.classLoader = new R2dbcMapperClassLoader();
        this.generator = new R2dbcMapperClassGenerator();
    }

    @SuppressWarnings("unchecked")
    public <T> R2dbcEntityMapper<T> getMapper(Class<T> entityClass) {
        return (R2dbcEntityMapper<T>) mapperCache.computeIfAbsent(entityClass, this::createMapper);
    }

    @SuppressWarnings("unchecked")
    public <T> R2dbcEntityMapper<T> register(R2dbcMappingConfig config) {
        Class<?> entityClass = config.getEntityClass();
        R2dbcEntityMapper<?> mapper = createMapper(config);
        mapperCache.put(entityClass, mapper);
        configCache.put(entityClass, config);
        return (R2dbcEntityMapper<T>) mapper;
    }

    public boolean hasMapper(Class<?> entityClass) {
        return mapperCache.containsKey(entityClass);
    }

    public void clear() {
        mapperCache.clear();
        configCache.clear();
    }

    private <T> R2dbcEntityMapper<T> createMapper(Class<T> entityClass) {
        R2dbcMappingConfig config = configCache.computeIfAbsent(entityClass, this::autoConfigure);
        return createMapper(config);
    }

    @SuppressWarnings("unchecked")
    private <T> R2dbcEntityMapper<T> createMapper(R2dbcMappingConfig config) {
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);

        try {
            Class<?> mapperClass = classLoader.defineClass(className, bytecode);
            return (R2dbcEntityMapper<T>) mapperClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mapper for " 
                    + config.getEntityClass().getName(), e);
        }
    }

    private <T> R2dbcMappingConfig autoConfigure(Class<T> entityClass) {
        String tableName = inferTableName(entityClass);
        R2dbcMappingConfig config = new R2dbcMappingConfig(entityClass, tableName);

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            if (fieldName.equals("id")) {
                config.idField(fieldName, fieldType);
            } else {
                config.field(fieldName, fieldType);
            }
        }

        return config;
    }

    private String inferTableName(Class<?> entityClass) {
        String simpleName = entityClass.getSimpleName();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
}