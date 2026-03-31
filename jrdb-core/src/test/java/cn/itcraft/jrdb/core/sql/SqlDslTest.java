package cn.itcraft.jrdb.core.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import cn.itcraft.jrdb.core.sql.dsl.*;

@DisplayName("SqlDsl Tests")
class SqlDslTest {

    @Test
    @DisplayName("Should create SELECT builder")
    void shouldCreateSelectBuilder() {
        BoundSql sql = SqlDsl.select("id", "name")
            .from("users")
            .build();
        assertEquals("SELECT id, name FROM users", sql.getSql());
    }

    @Test
    @DisplayName("Should create SELECT ALL builder")
    void shouldCreateSelectAllBuilder() {
        BoundSql sql = SqlDsl.selectAll()
            .from("users")
            .build();
        assertEquals("SELECT * FROM users", sql.getSql());
    }

    @Test
    @DisplayName("Should create INSERT builder")
    void shouldCreateInsertBuilder() {
        BoundSql sql = SqlDsl.insertInto("users")
            .columns("name")
            .values("John")
            .build();
        assertEquals("INSERT INTO users (name) VALUES (?)", sql.getSql());
    }

    @Test
    @DisplayName("Should create UPDATE builder")
    void shouldCreateUpdateBuilder() {
        BoundSql sql = SqlDsl.update("users")
            .set("name", "John")
            .build();
        assertEquals("UPDATE users SET name = ?", sql.getSql());
    }

    @Test
    @DisplayName("Should create DELETE builder")
    void shouldCreateDeleteBuilder() {
        BoundSql sql = SqlDsl.deleteFrom("users")
            .where("id = ?", 1)
            .build();
        assertEquals("DELETE FROM users WHERE id = ?", sql.getSql());
    }
}