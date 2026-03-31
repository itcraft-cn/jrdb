package cn.itcraft.jrdb.core.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;

@DisplayName("TypeHandlerRegistry Tests")
class TypeHandlerRegistryTest {

    private TypeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeHandlerRegistry();
    }

    @Test
    @DisplayName("Should register and retrieve handler by Java type")
    void shouldRegisterAndGetByJavaType() {
        TypeHandler<String> handler = new TestStringHandler();
        registry.register(String.class, handler);
        
        TypeHandler<?> retrieved = registry.getTypeHandler(String.class);
        assertNotNull(retrieved);
        assertEquals(String.class, retrieved.getJavaType());
    }

    @Test
    @DisplayName("Should register and retrieve handler by SQL type")
    void shouldRegisterAndGetBySqlType() {
        TypeHandler<String> handler = new TestStringHandler();
        registry.register(java.sql.Types.VARCHAR, handler);
        
        TypeHandler<?> retrieved = registry.getTypeHandler(java.sql.Types.VARCHAR);
        assertNotNull(retrieved);
    }

    @Test
    @DisplayName("Should return null for unregistered type")
    void shouldReturnNullForUnregisteredType() {
        TypeHandler<?> retrieved = registry.getTypeHandler(Object.class);
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Should check if type handler exists")
    void shouldCheckIfTypeHandlerExists() {
        TypeHandler<String> handler = new TestStringHandler();
        registry.register(String.class, handler);
        
        assertTrue(registry.hasTypeHandler(String.class));
        assertFalse(registry.hasTypeHandler(Object.class));
    }

    @Test
    @DisplayName("Should have built-in Integer handler")
    void shouldHaveBuiltInIntegerHandler() {
        assertNotNull(registry.getTypeHandler(Integer.class));
        assertTrue(registry.hasTypeHandler(Integer.class));
    }

    @Test
    @DisplayName("Should have built-in Long handler")
    void shouldHaveBuiltInLongHandler() {
        assertNotNull(registry.getTypeHandler(Long.class));
        assertTrue(registry.hasTypeHandler(Long.class));
    }

    @Test
    @DisplayName("Should have built-in String handler")
    void shouldHaveBuiltInStringHandler() {
        assertNotNull(registry.getTypeHandler(String.class));
        assertTrue(registry.hasTypeHandler(String.class));
    }

    @Test
    @DisplayName("Should have built-in Boolean handler")
    void shouldHaveBuiltInBooleanHandler() {
        assertNotNull(registry.getTypeHandler(Boolean.class));
        assertTrue(registry.hasTypeHandler(Boolean.class));
    }

    @Test
    @DisplayName("Should have built-in Date handler")
    void shouldHaveBuiltInDateHandler() {
        assertNotNull(registry.getTypeHandler(Date.class));
        assertTrue(registry.hasTypeHandler(Date.class));
    }

    @Test
    @DisplayName("Should have built-in BigDecimal handler")
    void shouldHaveBuiltInBigDecimalHandler() {
        assertNotNull(registry.getTypeHandler(BigDecimal.class));
        assertTrue(registry.hasTypeHandler(BigDecimal.class));
    }

    private static class TestStringHandler implements TypeHandler<String> {
        @Override
        public String getResult(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
            return rs.getString(columnName);
        }

        @Override
        public String getResult(java.sql.ResultSet rs, int columnIndex) throws java.sql.SQLException {
            return rs.getString(columnIndex);
        }

        @Override
        public void setParameter(java.sql.PreparedStatement ps, int i, String parameter) throws java.sql.SQLException {
            if (parameter == null) {
                ps.setNull(i, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i, parameter);
            }
        }

        @Override
        public Class<String> getJavaType() {
            return String.class;
        }
    }
}