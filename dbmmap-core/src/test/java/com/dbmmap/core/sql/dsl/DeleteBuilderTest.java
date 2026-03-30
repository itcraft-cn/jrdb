package com.dbmmap.core.sql.dsl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import com.dbmmap.core.sql.BoundSql;

@DisplayName("DeleteBuilder Tests")
class DeleteBuilderTest {

    @Test
    @DisplayName("Should build simple DELETE")
    void shouldBuildSimpleDelete() {
        BoundSql sql = new DeleteBuilder("users")
            .build();
        assertEquals("DELETE FROM users", sql.getSql());
    }

    @Test
    @DisplayName("Should build DELETE with WHERE")
    void shouldBuildDeleteWithWhere() {
        BoundSql sql = new DeleteBuilder("users")
            .where("id = ?", 1)
            .build();
        assertEquals("DELETE FROM users WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{1}, sql.getParams());
    }

    @Test
    @DisplayName("Should build DELETE with multiple WHERE clauses")
    void shouldBuildDeleteWithMultipleWhere() {
        BoundSql sql = new DeleteBuilder("users")
            .where("status = ?", "inactive")
            .where("created_at < ?", "2023-01-01")
            .build();
        assertEquals("DELETE FROM users WHERE status = ? AND created_at < ?", sql.getSql());
        assertArrayEquals(new Object[]{"inactive", "2023-01-01"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build DELETE with conditional WHERE")
    void shouldBuildDeleteWithConditionalWhere() {
        BoundSql sql = new DeleteBuilder("users")
            .whereIf(true, "id = ?", 1)
            .whereIf(false, "status = ?", "active")
            .build();
        assertEquals("DELETE FROM users WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{1}, sql.getParams());
    }

    @Test
    @DisplayName("Should handle DELETE with no WHERE clause")
    void shouldHandleDeleteWithNoWhere() {
        BoundSql sql = new DeleteBuilder("users")
            .build();
        assertEquals("DELETE FROM users", sql.getSql());
        assertArrayEquals(new Object[0], sql.getParams());
    }
}