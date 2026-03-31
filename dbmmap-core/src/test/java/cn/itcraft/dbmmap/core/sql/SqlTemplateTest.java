package cn.itcraft.dbmmap.core.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

@DisplayName("SqlTemplate Tests")
class SqlTemplateTest {

    @Test
    @DisplayName("Should process parameter placeholder")
    void shouldProcessParameterPlaceholder() {
        String template = "SELECT * FROM users WHERE id = #{id}";
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertEquals("SELECT * FROM users WHERE id = ?", boundSql.getSql());
        assertArrayEquals(new Object[]{1}, boundSql.getParams());
    }

    @Test
    @DisplayName("Should process string replacement")
    void shouldProcessStringReplacement() {
        String template = "SELECT * FROM ${table} WHERE id = #{id}";
        Map<String, Object> params = new HashMap<>();
        params.put("table", "users");
        params.put("id", 1);
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertEquals("SELECT * FROM users WHERE id = ?", boundSql.getSql());
        assertArrayEquals(new Object[]{1}, boundSql.getParams());
    }

    @Test
    @DisplayName("Should handle multiple parameters")
    void shouldHandleMultipleParameters() {
        String template = "SELECT * FROM users WHERE id = #{id} AND name = #{name}";
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("name", "test");
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", boundSql.getSql());
        assertArrayEquals(new Object[]{1, "test"}, boundSql.getParams());
    }

    @Test
    @DisplayName("Should handle nested property")
    void shouldHandleNestedProperty() {
        String template = "SELECT * FROM users WHERE name = #{user.name}";
        Map<String, Object> params = new HashMap<>();
        TestUser user = new TestUser();
        user.name = "test";
        params.put("user", user);
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertEquals("SELECT * FROM users WHERE name = ?", boundSql.getSql());
        assertArrayEquals(new Object[]{"test"}, boundSql.getParams());
    }

    @Test
    @DisplayName("Should return BoundSql")
    void shouldReturnBoundSql() {
        String template = "SELECT * FROM users";
        Map<String, Object> params = new HashMap<>();
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertNotNull(boundSql);
        assertTrue(boundSql instanceof BoundSql);
    }

    @Test
    @DisplayName("Should handle missing replacement value")
    void shouldHandleMissingReplacementValue() {
        String template = "SELECT * FROM ${table}";
        Map<String, Object> params = new HashMap<>();
        
        BoundSql boundSql = SqlTemplate.process(template, params);
        
        assertEquals("SELECT * FROM ", boundSql.getSql());
    }

    static class TestUser {
        public String name;
    }
}