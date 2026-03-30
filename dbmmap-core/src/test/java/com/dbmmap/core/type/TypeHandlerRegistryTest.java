package com.dbmmap.core.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

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
        TypeHandler<?> retrieved = registry.getTypeHandler(String.class);
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Should check if type handler exists")
    void shouldCheckIfTypeHandlerExists() {
        TypeHandler<String> handler = new TestStringHandler();
        registry.register(String.class, handler);
        
        assertTrue(registry.hasTypeHandler(String.class));
        assertFalse(registry.hasTypeHandler(Integer.class));
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