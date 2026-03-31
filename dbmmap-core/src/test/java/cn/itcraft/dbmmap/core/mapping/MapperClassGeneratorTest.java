package cn.itcraft.dbmmap.core.mapping;

import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapperClassGenerator.
 *
 * @author dbmmap
 * @since 1.0.0
 */
class MapperClassGeneratorTest {

    private MapperClassGenerator generator;
    private TypeHandlerRegistry registry;
    private MapperClassLoader classLoader;

    @BeforeEach
    void setUp() {
        registry = new TypeHandlerRegistry();
        generator = new MapperClassGenerator();
        classLoader = new MapperClassLoader();
    }

    @Test
    @DisplayName("generate produces valid bytecode")
    void testGenerateProducesBytecode() {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    @Test
    @DisplayName("generated class implements EntityMapper")
    void testGeneratedClassImplementsEntityMapper() throws Exception {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, bytecode);
        assertTrue(EntityMapper.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("generated class has mapFromResultSet method")
    void testGeneratedClassHasMapFromResultSet() throws Exception {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, bytecode);
        assertNotNull(clazz.getMethod("mapFromResultSet", ResultSet.class));
    }

    @Test
    @DisplayName("generated class has mapToRow method")
    void testGeneratedClassHasMapToRow() throws Exception {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, bytecode);
        assertNotNull(clazz.getMethod("mapToRow", Object.class));
    }

    @Test
    @DisplayName("generated mapper can be instantiated")
    void testGeneratedMapperCanBeInstantiated() throws Exception {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        String className = generator.getGeneratedClassName(config);
        
        Class<?> clazz = classLoader.defineClass(className, bytecode);
        Object mapper = clazz.getDeclaredConstructor().newInstance();
        assertNotNull(mapper);
    }

    @Test
    @DisplayName("getGeneratedClassName produces correct name")
    void testGetGeneratedClassName() {
        MappingConfig config = createTestConfig();
        
        String className = generator.getGeneratedClassName(config);
        assertTrue(className.contains("User"));
        assertTrue(className.endsWith("Mapper"));
    }

    @Test
    @DisplayName("generated class has correct superclass")
    void testGeneratedClassSuperclass() throws Exception {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        
        ClassReader reader = new ClassReader(bytecode);
        SuperclassChecker checker = new SuperclassChecker();
        reader.accept(checker, 0);
        
        assertEquals("java/lang/Object", checker.superclass);
    }

    @Test
    @DisplayName("bytecode contains EntityMapper interface")
    void testBytecodeContainsInterface() {
        MappingConfig config = createTestConfig();
        
        byte[] bytecode = generator.generate(config);
        
        ClassReader reader = new ClassReader(bytecode);
        InterfaceChecker checker = new InterfaceChecker();
        reader.accept(checker, 0);
        
        assertTrue(checker.interfaces.contains("cn/itcraft/dbmmap/core/mapping/EntityMapper"));
    }

    private MappingConfig createTestConfig() {
        MappingConfig config = new MappingConfig(User.class, "users", registry);
        config.idField("id", Long.class);
        config.field("name", String.class);
        return config;
    }

    static class User {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static byte[] generateTestClassBytes() {
        MapperClassGenerator gen = new MapperClassGenerator();
        TypeHandlerRegistry reg = new TypeHandlerRegistry();
        MappingConfig config = new MappingConfig(User.class, "users", reg);
        config.idField("id", Long.class);
        config.field("name", String.class);
        return gen.generate(config);
    }

    static class SuperclassChecker extends ClassVisitor {
        String superclass;

        SuperclassChecker() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superclass = superName;
        }
    }

    static class InterfaceChecker extends ClassVisitor {
        java.util.List<String> interfaces = new java.util.ArrayList<>();

        InterfaceChecker() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            if (interfaces != null) {
                for (String intf : interfaces) {
                    this.interfaces.add(intf);
                }
            }
        }
    }
}