package cn.itcraft.jrdb.jdbc.mapping;

import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体映射器注册表，管理EntityMapper实例的自动配置与缓存。
 *
 * <p>支持两种注册方式：
 * <ul>
 *     <li>显式注册：通过MappingConfig手动配置映射规则</li>
 *     <li>自动配置：基于实体类反射自动推断映射规则</li>
 * </ul>
 *
 * @author Helly Guo
 * @since 1.0.0
 */
public class MapperRegistry {

    private final TypeHandlerRegistry typeHandlerRegistry;
    private final MapperClassLoader classLoader;
    private final MapperClassGenerator generator;

    private final Map<Class<?>, EntityMapper<?>> mapperCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, MappingConfig> configCache = new ConcurrentHashMap<>();

    /**
     * 构造MapperRegistry。
     *
     * @param typeHandlerRegistry 类型处理器注册表
     * @throws IllegalArgumentException 如果typeHandlerRegistry为null
     */
    public MapperRegistry(TypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = java.util.Objects.requireNonNull(typeHandlerRegistry,
                "typeHandlerRegistry cannot be null");
        this.classLoader = new MapperClassLoader();
        this.generator = new MapperClassGenerator();
    }

    /**
     * 获取或创建实体类的映射器。
     *
     * <p>如果映射器已缓存则直接返回，否则自动配置并生成映射器。
     *
     * @param <T> 实体类型
     * @param entityClass 实体类
     * @return EntityMapper实例
     * @throws IllegalArgumentException 如果entityClass为null
     */
    @SuppressWarnings("unchecked")
    public <T> EntityMapper<T> getMapper(Class<T> entityClass) {
        java.util.Objects.requireNonNull(entityClass, "entityClass cannot be null");

        return (EntityMapper<T>) mapperCache.computeIfAbsent(entityClass, this::createMapper);
    }

    /**
     * 注册带显式配置的映射器。
     *
     * <p>基于提供的MappingConfig创建并缓存映射器。
     *
     * @param <T> 实体类型
     * @param config 映射配置
     * @return 创建的EntityMapper
     * @throws IllegalArgumentException 如果config为null
     */
    @SuppressWarnings("unchecked")
    public <T> EntityMapper<T> register(MappingConfig config) {
        java.util.Objects.requireNonNull(config, "config cannot be null");

        Class<?> entityClass = config.getEntityClass();
        EntityMapper<?> mapper = createMapper(config);
        mapperCache.put(entityClass, mapper);
        configCache.put(entityClass, config);

        return (EntityMapper<T>) mapper;
    }

    /**
     * 检查是否已注册指定实体类的映射器。
     *
     * @param entityClass 实体类
     * @return 是否已注册
     */
    public boolean hasMapper(Class<?> entityClass) {
        return mapperCache.containsKey(entityClass);
    }

    /**
     * 获取实体类的映射配置。
     *
     * @param entityClass 实体类
     * @return MappingConfig，如未配置则返回null
     */
    public MappingConfig getConfig(Class<?> entityClass) {
        return configCache.get(entityClass);
    }

    /**
     * 清空所有缓存的映射器和配置。
     */
    public void clear() {
        mapperCache.clear();
        configCache.clear();
    }

    /**
     * 获取缓存的映射器数量。
     *
     * @return 映射器数量
     */
    public int size() {
        return mapperCache.size();
    }

    private <T> EntityMapper<T> createMapper(Class<T> entityClass) {
        MappingConfig config = configCache.computeIfAbsent(entityClass, this::autoConfigure);
        return createMapper(config);
    }

    @SuppressWarnings("unchecked")
    private <T> EntityMapper<T> createMapper(MappingConfig config) {
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);

        try {
            Class<?> mapperClass = classLoader.defineClass(className, bytecode);
            return (EntityMapper<T>) mapperClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mapper for " 
                    + config.getEntityClass().getName(), e);
        }
    }

    private <T> MappingConfig autoConfigure(Class<T> entityClass) {
        String tableName = inferTableName(entityClass);
        MappingConfig config = new MappingConfig(entityClass, tableName, typeHandlerRegistry);

        Field[] fields = entityClass.getDeclaredFields();
        java.util.Arrays.sort(fields, (a, b) -> a.getName().compareTo(b.getName()));
        
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
