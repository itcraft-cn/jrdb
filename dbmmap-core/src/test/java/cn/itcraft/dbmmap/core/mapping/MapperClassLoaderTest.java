package cn.itcraft.dbmmap.core.mapping;

import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapperClassLoader.
 *
 * @author dbmmap
 * @since 1.0.0
 */
class MapperClassLoaderTest {

    private MapperClassLoader classLoader;
    private MapperClassGenerator generator;
    private TypeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        classLoader = new MapperClassLoader();
        generator = new MapperClassGenerator();
        registry = new TypeHandlerRegistry();
    }

    @Test
    @DisplayName("defineClass loads a valid class")
    void testDefineClass() throws Exception {
        MappingConfig config = createTestConfig();
        byte[] classBytes = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, classBytes);
        assertNotNull(clazz);
        assertEquals(className, clazz.getName());
    }

    @Test
    @DisplayName("defineClass returns same class if already defined")
    void testDefineClassAlreadyDefined() throws Exception {
        MappingConfig config = createTestConfig();
        byte[] classBytes = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> first = classLoader.defineClass(className, classBytes);
        Class<?> second = classLoader.defineClass(className, classBytes);
        
        assertSame(first, second);
    }

    @Test
    @DisplayName("defined class can be instantiated")
    void testDefinedClassInstantiation() throws Exception {
        MappingConfig config = createTestConfig();
        byte[] classBytes = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, classBytes);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("defineClass throws for invalid bytes")
    void testDefineClassInvalidBytes() {
        byte[] invalidBytes = new byte[]{0x00, 0x01, 0x02};
        
        assertThrows(ClassFormatError.class, () -> 
            classLoader.defineClass("cn.itcraft.dbmmap.core.mapping.Invalid", invalidBytes));
    }

    private MappingConfig createTestConfig() {
        MappingConfig config = new MappingConfig(TestEntity.class, "test_entity", registry);
        config.idField("id", Long.class);
        config.field("name", String.class);
        return config;
    }

    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}