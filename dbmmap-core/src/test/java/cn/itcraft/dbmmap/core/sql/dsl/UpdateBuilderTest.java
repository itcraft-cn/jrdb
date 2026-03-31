package cn.itcraft.dbmmap.core.sql.dsl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import cn.itcraft.dbmmap.core.sql.BoundSql;

@DisplayName("UpdateBuilder Tests")
class UpdateBuilderTest {

    @Test
    @DisplayName("Should build simple UPDATE")
    void shouldBuildSimpleUpdate() {
        BoundSql sql = new UpdateBuilder("users")
            .set("name", "John")
            .build();
        assertEquals("UPDATE users SET name = ?", sql.getSql());
        assertArrayEquals(new Object[]{"John"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build UPDATE with multiple SETs")
    void shouldBuildUpdateWithMultipleSets() {
        BoundSql sql = new UpdateBuilder("users")
            .set("name", "John")
            .set("email", "john@example.com")
            .build();
        assertEquals("UPDATE users SET name = ?, email = ?", sql.getSql());
        assertArrayEquals(new Object[]{"John", "john@example.com"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build UPDATE with WHERE")
    void shouldBuildUpdateWithWhere() {
        BoundSql sql = new UpdateBuilder("users")
            .set("name", "John")
            .where("id = ?", 1)
            .build();
        assertEquals("UPDATE users SET name = ? WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{"John", 1}, sql.getParams());
    }

    @Test
    @DisplayName("Should build UPDATE with multiple WHERE clauses")
    void shouldBuildUpdateWithMultipleWhere() {
        BoundSql sql = new UpdateBuilder("users")
            .set("status", "inactive")
            .where("status = ?", "active")
            .where("last_login < ?", "2024-01-01")
            .build();
        assertEquals("UPDATE users SET status = ? WHERE status = ? AND last_login < ?", sql.getSql());
        assertArrayEquals(new Object[]{"inactive", "active", "2024-01-01"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build UPDATE with conditional WHERE")
    void shouldBuildUpdateWithConditionalWhere() {
        BoundSql sql = new UpdateBuilder("users")
            .set("name", "John")
            .whereIf(true, "id = ?", 1)
            .whereIf(false, "status = ?", "active")
            .build();
        assertEquals("UPDATE users SET name = ? WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{"John", 1}, sql.getParams());
    }

    @Test
    @DisplayName("Should build UPDATE with raw SET clause")
    void shouldBuildUpdateWithRawSet() {
        BoundSql sql = new UpdateBuilder("users")
            .setRaw("updated_at = NOW()")
            .where("id = ?", 1)
            .build();
        assertEquals("UPDATE users SET updated_at = NOW() WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{1}, sql.getParams());
    }
}