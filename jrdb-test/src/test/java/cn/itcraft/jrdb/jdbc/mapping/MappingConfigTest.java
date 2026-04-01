package cn.itcraft.jrdb.jdbc.mapping;

import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import cn.itcraft.jrdb.jdbc.type.handlers.StringTypeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MappingConfig.
 *
 * @author jrdb
 * @since 1.0.0
 */
class MappingConfigTest {

    private MappingConfig config;
    private TypeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeHandlerRegistry();
        config = new MappingConfig(User.class, "users", registry);
    }

    @Test
    @DisplayName("Constructor sets basic properties")
    void testConstructor() {
        assertEquals(User.class, config.getEntityClass());
        assertEquals("users", config.getTableName());
        assertNotNull(config.getTypeHandlerRegistry());
        assertNotNull(config.getFields());
        assertTrue(config.getFields().isEmpty());
    }

    @Test
    @DisplayName("field(fieldName, columnName, javaType) adds field")
    void testFieldWithColumnName() {
        config.field("userId", "user_id", Long.class);
        
        List<MappingField> fields = config.getFields();
        assertEquals(1, fields.size());
        assertEquals("userId", fields.get(0).getFieldName());
        assertEquals("user_id", fields.get(0).getColumnName());
        assertEquals(Long.class, fields.get(0).getJavaType());
    }

    @Test
    @DisplayName("field(fieldName, javaType) auto-converts column name")
    void testFieldAutoConvert() {
        config.field("userId", Long.class);
        
        List<MappingField> fields = config.getFields();
        assertEquals(1, fields.size());
        assertEquals("userId", fields.get(0).getFieldName());
        assertEquals("user_id", fields.get(0).getColumnName());
        assertEquals(Long.class, fields.get(0).getJavaType());
    }

    @Test
    @DisplayName("idField(fieldName, javaType) marks as id")
    void testIdField() {
        config.idField("userId", Long.class);
        
        List<MappingField> fields = config.getFields();
        assertEquals(1, fields.size());
        assertTrue(fields.get(0).isId());
        assertEquals("user_id", fields.get(0).getColumnName());
    }

    @Test
    @DisplayName("idField(fieldName, columnName, javaType) marks as id")
    void testIdFieldWithColumnName() {
        config.idField("userId", "pk_user_id", Long.class);
        
        List<MappingField> fields = config.getFields();
        assertEquals(1, fields.size());
        assertTrue(fields.get(0).isId());
        assertEquals("pk_user_id", fields.get(0).getColumnName());
    }

    @Test
    @DisplayName("getFieldByFieldName returns correct field")
    void testGetFieldByFieldName() {
        config.field("userId", Long.class);
        config.field("userName", String.class);
        
        MappingField field = config.getFieldByFieldName("userName");
        assertNotNull(field);
        assertEquals("userName", field.getFieldName());
        assertNull(config.getFieldByFieldName("nonexistent"));
    }

    @Test
    @DisplayName("getFieldByColumnName returns correct field")
    void testGetFieldByColumnName() {
        config.field("userId", Long.class);
        config.field("userName", String.class);
        
        MappingField field = config.getFieldByColumnName("user_name");
        assertNotNull(field);
        assertEquals("userName", field.getFieldName());
        assertNull(config.getFieldByColumnName("nonexistent"));
    }

    @Test
    @DisplayName("setIdField sets the id field")
    void testSetIdField() {
        config.field("userId", Long.class);
        config.setIdField("userId");
        
        assertTrue(config.getFieldByFieldName("userId").isId());
    }

    @Test
    @DisplayName("setIdField throws if field not found")
    void testSetIdFieldNotFound() {
        assertThrows(IllegalArgumentException.class, () -> config.setIdField("nonexistent"));
    }

    @Test
    @DisplayName("nameStrategy defaults to DefaultNameStrategy")
    void testDefaultNameStrategy() {
        assertNotNull(config.getNameStrategy());
        assertEquals("default", config.getNameStrategy().getName());
    }

    @Test
    @DisplayName("setNameStrategy changes strategy")
    void testSetNameStrategy() {
        NameStrategy custom = new CustomNameStrategy();
        config.setNameStrategy(custom);
        assertEquals(custom, config.getNameStrategy());
    }

    @Test
    @DisplayName("Multiple fields are ordered correctly")
    void testMultipleFields() {
        config.field("id", Long.class);
        config.field("name", String.class);
        config.field("email", String.class);
        
        List<MappingField> fields = config.getFields();
        assertEquals(3, fields.size());
        assertEquals("id", fields.get(0).getFieldName());
        assertEquals("name", fields.get(1).getFieldName());
        assertEquals("email", fields.get(2).getFieldName());
    }

    static class User {
        private Long userId;
        private String userName;
    }

    static class CustomNameStrategy implements NameStrategy {
        @Override
        public String columnNameToFieldName(String columnName) {
            return columnName;
        }

        @Override
        public String fieldNameToColumnName(String fieldName) {
            return fieldName;
        }

        @Override
        public String getName() {
            return "custom";
        }
    }
}