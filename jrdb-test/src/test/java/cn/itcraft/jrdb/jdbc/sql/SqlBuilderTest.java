package cn.itcraft.jrdb.jdbc.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SqlBuilder Tests")
class SqlBuilderTest {

    private SqlBuilder builder;

    @BeforeEach
    void setUp() {
        builder = SqlBuilder.create();
    }

    @Test
    @DisplayName("Should build simple SQL")
    void shouldBuildSimpleSql() {
        builder.append("SELECT * FROM users");
        assertEquals("SELECT * FROM users", builder.build());
    }

    @Test
    @DisplayName("Should collect params")
    void shouldCollectParams() {
        builder.append("SELECT * FROM users WHERE id = ?", 1);
        assertEquals("SELECT * FROM users WHERE id = ?", builder.build());
        assertArrayEquals(new Object[]{1}, builder.getParams());
    }

    @Test
    @DisplayName("Should append conditionally")
    void shouldAppendConditionally() {
        builder.append("SELECT * FROM users");
        builder.appendIf(true, "WHERE status = ?", "active");
        assertEquals("SELECT * FROM users WHERE status = ?", builder.build());
        assertArrayEquals(new Object[]{"active"}, builder.getParams());
        
        builder.reset();
        builder.append("SELECT * FROM users");
        builder.appendIf(false, "WHERE status = ?", "active");
        assertEquals("SELECT * FROM users", builder.build());
        assertArrayEquals(new Object[0], builder.getParams());
    }

    @Test
    @DisplayName("Should reset builder")
    void shouldReset() {
        builder.append("SELECT * FROM users WHERE id = ?", 1);
        assertFalse(builder.isEmpty());
        
        builder.reset();
        assertTrue(builder.isEmpty());
        assertEquals("", builder.build());
        assertArrayEquals(new Object[0], builder.getParams());
    }

    @Test
    @DisplayName("Should append multiple fragments with space")
    void shouldAppendMultipleFragmentsWithSpace() {
        builder.append("SELECT *");
        builder.append("FROM users");
        builder.append("WHERE id = ?", 1);
        assertEquals("SELECT * FROM users WHERE id = ?", builder.build());
    }

    @Test
    @DisplayName("Should collect multiple params")
    void shouldCollectMultipleParams() {
        builder.append("SELECT * FROM users WHERE id = ? AND status = ?", 1, "active");
        assertArrayEquals(new Object[]{1, "active"}, builder.getParams());
    }

    @Test
    @DisplayName("Should handle empty builder")
    void shouldHandleEmptyBuilder() {
        assertTrue(builder.isEmpty());
        assertEquals("", builder.build());
        assertArrayEquals(new Object[0], builder.getParams());
    }
}