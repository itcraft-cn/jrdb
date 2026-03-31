package cn.itcraft.dbmmap.core.mapping;

import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for entity mappers with automatic configuration.
 * <p>
 * Manages EntityMapper instances for entity classes, providing automatic
 * mapper generation and caching. Supports both explicit registration via
 * MappingConfig and automatic configuration from entity class reflection.
 * </p>
 *
 * @author dbmmap
 * @since 1.0.0
 */
public class MapperRegistry {

    private final TypeHandlerRegistry typeHandlerRegistry;
    private final MapperClassLoader classLoader;
    private final MapperClassGenerator generator;

    private final Map<Class<?>, EntityMapper<?>> mapperCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, MappingConfig> configCache = new ConcurrentHashMap<>();

    /**
     * Constructs a MapperRegistry with the specified TypeHandlerRegistry.
     *
     * @param typeHandlerRegistry the registry for type handlers
     * @throws IllegalArgumentException if typeHandlerRegistry is null
     */
    public MapperRegistry(TypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = java.util.Objects.requireNonNull(typeHandlerRegistry,
                "typeHandlerRegistry cannot be null");
        this.classLoader = new MapperClassLoader();
        this.generator = new MapperClassGenerator();
    }

    /**
     * Gets or creates a mapper for the specified entity class.
     * <p>
     * If a mapper is already cached, returns it directly. Otherwise,
     * automatically configures the mapping from the entity class using
     * reflection and generates the mapper.
     * </p>
     *
     * @param <T> the entity type
     * @param entityClass the entity class
     * @return the EntityMapper for the entity class
     * @throws IllegalArgumentException if entityClass is null
     */
    @SuppressWarnings("unchecked")
    public <T> EntityMapper<T> getMapper(Class<T> entityClass) {
        java.util.Objects.requireNonNull(entityClass, "entityClass cannot be null");

        return (EntityMapper<T>) mapperCache.computeIfAbsent(entityClass, this::createMapper);
    }

    /**
     * Registers a mapper with explicit configuration.
     * <p>
     * Creates and caches a mapper based on the provided MappingConfig.
     * This allows fine-grained control over the mapping behavior.
     * </p>
     *
     * @param <T> the entity type
     * @param config the mapping configuration
     * @return the created EntityMapper
     * @throws IllegalArgumentException if config is null
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
     * Checks if a mapper is registered for the specified entity class.
     *
     * @param entityClass the entity class to check
     * @return true if a mapper is registered
     */
    public boolean hasMapper(Class<?> entityClass) {
        return mapperCache.containsKey(entityClass);
    }

    /**
     * Gets the mapping configuration for an entity class.
     *
     * @param entityClass the entity class
     * @return the MappingConfig, or null if not configured
     */
    public MappingConfig getConfig(Class<?> entityClass) {
        return configCache.get(entityClass);
    }

    /**
     * Clears all cached mappers and configurations.
     */
    public void clear() {
        mapperCache.clear();
        configCache.clear();
    }

    /**
     * Gets the number of cached mappers.
     *
     * @return the number of cached mappers
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