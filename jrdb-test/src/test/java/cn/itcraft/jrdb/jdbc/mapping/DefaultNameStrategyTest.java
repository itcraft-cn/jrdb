package cn.itcraft.jrdb.jdbc.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultNameStrategy.
 *
 * @author jrdb
 * @since 1.0.0
 */
class DefaultNameStrategyTest {

    private DefaultNameStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultNameStrategy();
    }

    @Test
    @DisplayName("columnNameToFieldName - user_id -> userId")
    void testColumnNameToFieldName_basic() {
        assertEquals("userId", strategy.columnNameToFieldName("user_id"));
    }

    @Test
    @DisplayName("columnNameToFieldName - user_name -> userName")
    void testColumnNameToFieldName_userName() {
        assertEquals("userName", strategy.columnNameToFieldName("user_name"));
    }

    @Test
    @DisplayName("columnNameToFieldName - id -> id")
    void testColumnNameToFieldName_singleWord() {
        assertEquals("id", strategy.columnNameToFieldName("id"));
    }

    @Test
    @DisplayName("columnNameToFieldName - created_at -> createdAt")
    void testColumnNameToFieldName_createdAt() {
        assertEquals("createdAt", strategy.columnNameToFieldName("created_at"));
    }

    @Test
    @DisplayName("columnNameToFieldName - is_active -> isActive")
    void testColumnNameToFieldName_isActive() {
        assertEquals("isActive", strategy.columnNameToFieldName("is_active"));
    }

    @Test
    @DisplayName("columnNameToFieldName - consecutive underscores: user__name -> userName")
    void testColumnNameToFieldName_consecutiveUnderscores() {
        assertEquals("userName", strategy.columnNameToFieldName("user__name"));
    }

    @Test
    @DisplayName("columnNameToFieldName - leading underscore: _private -> private")
    void testColumnNameToFieldName_leadingUnderscore() {
        assertEquals("private", strategy.columnNameToFieldName("_private"));
    }

    @Test
    @DisplayName("columnNameToFieldName - trailing underscore: name_ -> name")
    void testColumnNameToFieldName_trailingUnderscore() {
        assertEquals("name", strategy.columnNameToFieldName("name_"));
    }

    @Test
    @DisplayName("columnNameToFieldName - null returns null")
    void testColumnNameToFieldName_null() {
        assertNull(strategy.columnNameToFieldName(null));
    }

    @Test
    @DisplayName("columnNameToFieldName - empty string returns empty string")
    void testColumnNameToFieldName_empty() {
        assertEquals("", strategy.columnNameToFieldName(""));
    }

    @Test
    @DisplayName("fieldNameToColumnName - userId -> user_id")
    void testFieldNameToColumnName_basic() {
        assertEquals("user_id", strategy.fieldNameToColumnName("userId"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - userName -> user_name")
    void testFieldNameToColumnName_userName() {
        assertEquals("user_name", strategy.fieldNameToColumnName("userName"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - id -> id")
    void testFieldNameToColumnName_singleWord() {
        assertEquals("id", strategy.fieldNameToColumnName("id"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - createdAt -> created_at")
    void testFieldNameToColumnName_createdAt() {
        assertEquals("created_at", strategy.fieldNameToColumnName("createdAt"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - isActive -> is_active")
    void testFieldNameToColumnName_isActive() {
        assertEquals("is_active", strategy.fieldNameToColumnName("isActive"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - multiple uppercase: userFirstName -> user_first_name")
    void testFieldNameToColumnName_multipleUppercase() {
        assertEquals("user_first_name", strategy.fieldNameToColumnName("userFirstName"));
    }

    @Test
    @DisplayName("fieldNameToColumnName - null returns null")
    void testFieldNameToColumnName_null() {
        assertNull(strategy.fieldNameToColumnName(null));
    }

    @Test
    @DisplayName("fieldNameToColumnName - empty string returns empty string")
    void testFieldNameToColumnName_empty() {
        assertEquals("", strategy.fieldNameToColumnName(""));
    }

    @Test
    @DisplayName("getName returns 'default'")
    void testGetName() {
        assertEquals("default", strategy.getName());
    }

    @Test
    @DisplayName("Round trip: user_id -> userId -> user_id")
    void testRoundTrip() {
        String original = "user_id";
        String fieldName = strategy.columnNameToFieldName(original);
        String backToColumn = strategy.fieldNameToColumnName(fieldName);
        assertEquals(original, backToColumn);
    }

    @Test
    @DisplayName("Complex name: created_by_user_id -> createdByUserId")
    void testColumnNameToFieldName_complex() {
        assertEquals("createdByUserId", strategy.columnNameToFieldName("created_by_user_id"));
    }
}