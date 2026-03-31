package cn.itcraft.dbmmap.core.mapping;

import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapperRegistry.
 *
 * @author dbmmap
 * @since 1.0.0
 */
class MapperRegistryTest {

    private MapperRegistry registry;
    private TypeHandlerRegistry typeHandlerRegistry;

    @BeforeEach
    void setUp() {
        typeHandlerRegistry = new TypeHandlerRegistry();
        registry = new MapperRegistry(typeHandlerRegistry);
    }

    @Test
    @DisplayName("getMapper returns mapper for entity class")
    void testGetMapper() {
        EntityMapper<?> mapper = registry.getMapper(TestUser.class);
        assertNotNull(mapper);
    }

    @Test
    @DisplayName("getMapper returns cached mapper on second call")
    void testGetMapperCached() {
        EntityMapper<?> first = registry.getMapper(TestUser.class);
        EntityMapper<?> second = registry.getMapper(TestUser.class);
        assertSame(first, second);
    }

    @Test
    @DisplayName("register creates mapper from config")
    void testRegister() {
        MappingConfig config = new MappingConfig(TestUser.class, "test_user", typeHandlerRegistry);
        config.idField("id", Long.class);
        config.field("name", String.class);
        
        EntityMapper<?> mapper = registry.register(config);
        assertNotNull(mapper);
    }

    @Test
    @DisplayName("register caches mapper")
    void testRegisterCaches() {
        MappingConfig config = new MappingConfig(TestUser.class, "test_user", typeHandlerRegistry);
        config.idField("id", Long.class);
        
        EntityMapper<?> first = registry.register(config);
        EntityMapper<?> second = registry.getMapper(TestUser.class);
        assertSame(first, second);
    }

    @Test
    @DisplayName("hasMapper returns false for unregistered class")
    void testHasMapperFalse() {
        assertFalse(registry.hasMapper(TestUser.class));
    }

    @Test
    @DisplayName("hasMapper returns true after registration")
    void testHasMapperTrue() {
        registry.getMapper(TestUser.class);
        assertTrue(registry.hasMapper(TestUser.class));
    }

    @Test
    @DisplayName("getMapper auto-configures fields from entity")
    void testAutoConfigureFields() {
        EntityMapper<?> mapper = registry.getMapper(TestProduct.class);
        assertNotNull(mapper);
    }

    @Test
    @DisplayName("clear removes all cached mappers")
    void testClear() {
        registry.getMapper(TestUser.class);
        assertTrue(registry.hasMapper(TestUser.class));
        
        registry.clear();
        assertFalse(registry.hasMapper(TestUser.class));
    }

    @Test
    @DisplayName("getConfig returns config after registration")
    void testGetConfig() {
        registry.getMapper(TestUser.class);
        MappingConfig config = registry.getConfig(TestUser.class);
        assertNotNull(config);
        assertEquals(TestUser.class, config.getEntityClass());
    }

    static class TestUser {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static class TestProduct {
        private Long productId;
        private String productName;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}