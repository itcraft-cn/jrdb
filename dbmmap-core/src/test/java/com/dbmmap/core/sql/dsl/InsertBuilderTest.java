package com.dbmmap.core.sql.dsl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import com.dbmmap.core.sql.BoundSql;

@DisplayName("InsertBuilder Tests")
class InsertBuilderTest {

    @Test
    @DisplayName("Should build simple INSERT")
    void shouldBuildSimpleInsert() {
        BoundSql sql = new InsertBuilder("users")
            .columns("name", "email")
            .values("John", "john@example.com")
            .build();
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", sql.getSql());
        assertArrayEquals(new Object[]{"John", "john@example.com"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build INSERT without explicit columns")
    void shouldBuildInsertWithoutExplicitColumns() {
        BoundSql sql = new InsertBuilder("users")
            .values("John", "john@example.com")
            .build();
        assertEquals("INSERT INTO users VALUES (?, ?)", sql.getSql());
        assertArrayEquals(new Object[]{"John", "john@example.com"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build batch INSERT")
    void shouldBuildBatchInsert() {
        BoundSql sql = new InsertBuilder("users")
            .columns("name", "email")
            .values("John", "john@example.com")
            .values("Jane", "jane@example.com")
            .build();
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?), (?, ?)", sql.getSql());
        assertArrayEquals(new Object[]{"John", "john@example.com", "Jane", "jane@example.com"}, sql.getParams());
    }

    @Test
    @DisplayName("Should throw exception when no values")
    void shouldThrowExceptionWhenNoValues() {
        InsertBuilder builder = new InsertBuilder("users")
            .columns("name", "email");
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("Should handle single column")
    void shouldHandleSingleColumn() {
        BoundSql sql = new InsertBuilder("users")
            .columns("name")
            .values("John")
            .build();
        assertEquals("INSERT INTO users (name) VALUES (?)", sql.getSql());
        assertArrayEquals(new Object[]{"John"}, sql.getParams());
    }
}