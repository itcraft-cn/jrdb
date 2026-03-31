package cn.itcraft.jrdb.core.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MappingField.
 *
 * @author jrdb
 * @since 1.0.0
 */
class MappingFieldTest {

    private MappingField field;

    @BeforeEach
    void setUp() {
        field = new MappingField("userId", "user_id", Long.class);
    }

    @Test
    @DisplayName("Constructor sets all fields correctly")
    void testConstructor() {
        assertEquals("userId", field.getFieldName());
        assertEquals("user_id", field.getColumnName());
        assertEquals(Long.class, field.getJavaType());
        assertFalse(field.isId());
        assertNull(field.getTypeHandler());
    }

    @Test
    @DisplayName("id() marks field as id")
    void testId() {
        MappingField result = field.id();
        assertTrue(field.isId());
        assertSame(field, result);
    }

    @Test
    @DisplayName("typeHandler() sets type handler")
    void testTypeHandler() {
        MappingField result = field.typeHandler(null);
        assertSame(field, result);
    }

    @Test
    @DisplayName("equals and hashCode based on fieldName")
    void testEqualsHashCode() {
        MappingField same = new MappingField("userId", "user_id", Long.class);
        MappingField different = new MappingField("userName", "user_name", String.class);

        assertEquals(field, same);
        assertNotEquals(field, different);
        assertEquals(field.hashCode(), same.hashCode());
    }

    @Test
    @DisplayName("toString contains all fields")
    void testToString() {
        String str = field.toString();
        assertTrue(str.contains("userId"));
        assertTrue(str.contains("user_id"));
        assertTrue(str.contains("Long"));
    }

    @Test
    @DisplayName("id field toString shows id=true")
    void testToStringWithId() {
        field.id();
        String str = field.toString();
        assertTrue(str.contains("id=true"));
    }
}