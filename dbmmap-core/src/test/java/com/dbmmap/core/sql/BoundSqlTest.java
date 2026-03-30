package com.dbmmap.core.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BoundSql Tests")
class BoundSqlTest {

    @Test
    @DisplayName("Should create with sql and params")
    void shouldCreateWithSqlAndParams() {
        BoundSql boundSql = new BoundSql("SELECT * FROM users WHERE id = ?", new Object[]{1});
        assertEquals("SELECT * FROM users WHERE id = ?", boundSql.getSql());
        assertArrayEquals(new Object[]{1}, boundSql.getParams());
    }

    @Test
    @DisplayName("Should handle null params")
    void shouldHandleNullParams() {
        BoundSql boundSql = new BoundSql("SELECT * FROM users", null);
        assertEquals("SELECT * FROM users", boundSql.getSql());
        assertArrayEquals(new Object[0], boundSql.getParams());
    }

    @Test
    @DisplayName("Should handle empty params")
    void shouldHandleEmptyParams() {
        BoundSql boundSql = new BoundSql("SELECT * FROM users", new Object[0]);
        assertEquals("SELECT * FROM users", boundSql.getSql());
        assertArrayEquals(new Object[0], boundSql.getParams());
    }
}