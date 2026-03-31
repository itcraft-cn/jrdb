package cn.itcraft.dbmmap.core.sql.dsl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import cn.itcraft.dbmmap.core.sql.BoundSql;

@DisplayName("SelectBuilder Tests")
class SelectBuilderTest {

    @Test
    @DisplayName("Should build simple SELECT")
    void shouldBuildSimpleSelect() {
        BoundSql sql = new SelectBuilder("id", "name")
            .from("users")
            .build();
        assertEquals("SELECT id, name FROM users", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with alias")
    void shouldBuildSelectWithAlias() {
        BoundSql sql = new SelectBuilder("*")
            .from("users", "u")
            .build();
        assertEquals("SELECT * FROM users u", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with WHERE")
    void shouldBuildSelectWithWhere() {
        BoundSql sql = new SelectBuilder("*")
            .from("users")
            .where("id = ?", 1)
            .build();
        assertEquals("SELECT * FROM users WHERE id = ?", sql.getSql());
        assertArrayEquals(new Object[]{1}, sql.getParams());
    }

    @Test
    @DisplayName("Should build SELECT with multiple WHERE clauses")
    void shouldBuildSelectWithMultipleWhere() {
        BoundSql sql = new SelectBuilder("*")
            .from("users")
            .where("id = ?", 1)
            .where("status = ?", "active")
            .build();
        assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", sql.getSql());
        assertArrayEquals(new Object[]{1, "active"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build SELECT with conditional WHERE")
    void shouldBuildSelectWithConditionalWhere() {
        BoundSql sql = new SelectBuilder("*")
            .from("users")
            .whereIf(true, "status = ?", "active")
            .whereIf(false, "deleted = ?", true)
            .build();
        assertEquals("SELECT * FROM users WHERE status = ?", sql.getSql());
        assertArrayEquals(new Object[]{"active"}, sql.getParams());
    }

    @Test
    @DisplayName("Should build SELECT with JOIN")
    void shouldBuildSelectWithJoin() {
        BoundSql sql = new SelectBuilder("*")
            .from("users", "u")
            .join("orders o", "u.id = o.user_id")
            .build();
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with LEFT JOIN")
    void shouldBuildSelectWithLeftJoin() {
        BoundSql sql = new SelectBuilder("*")
            .from("users", "u")
            .leftJoin("orders o", "u.id = o.user_id")
            .build();
        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with RIGHT JOIN")
    void shouldBuildSelectWithRightJoin() {
        BoundSql sql = new SelectBuilder("*")
            .from("users", "u")
            .rightJoin("orders o", "u.id = o.user_id")
            .build();
        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with GROUP BY")
    void shouldBuildSelectWithGroupBy() {
        BoundSql sql = new SelectBuilder("status", "COUNT(*)")
            .from("users")
            .groupBy("status")
            .build();
        assertEquals("SELECT status, COUNT(*) FROM users GROUP BY status", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with HAVING")
    void shouldBuildSelectWithHaving() {
        BoundSql sql = new SelectBuilder("status", "COUNT(*) as cnt")
            .from("users")
            .groupBy("status")
            .having("COUNT(*) > ?", 5)
            .build();
        assertEquals("SELECT status, COUNT(*) as cnt FROM users GROUP BY status HAVING COUNT(*) > ?", sql.getSql());
        assertArrayEquals(new Object[]{5}, sql.getParams());
    }

    @Test
    @DisplayName("Should build SELECT with ORDER BY")
    void shouldBuildSelectWithOrderBy() {
        BoundSql sql = new SelectBuilder("*")
            .from("users")
            .orderBy("name", "id DESC")
            .build();
        assertEquals("SELECT * FROM users ORDER BY name, id DESC", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with LIMIT and OFFSET")
    void shouldBuildSelectWithLimitOffset() {
        BoundSql sql = new SelectBuilder("*")
            .from("users")
            .limit(10)
            .offset(20)
            .build();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql.getSql());
    }

    @Test
    @DisplayName("Should build SELECT with all clauses")
    void shouldBuildSelectWithAllClauses() {
        BoundSql sql = new SelectBuilder("u.id", "u.name", "COUNT(o.id) as order_count")
            .from("users", "u")
            .leftJoin("orders o", "u.id = o.user_id")
            .where("u.status = ?", "active")
            .groupBy("u.id", "u.name")
            .having("COUNT(o.id) > ?", 0)
            .orderBy("u.name")
            .limit(10)
            .offset(0)
            .build();
        
        assertTrue(sql.getSql().contains("SELECT u.id, u.name, COUNT(o.id) as order_count"));
        assertTrue(sql.getSql().contains("FROM users u"));
        assertTrue(sql.getSql().contains("LEFT JOIN orders o ON u.id = o.user_id"));
        assertTrue(sql.getSql().contains("WHERE u.status = ?"));
        assertTrue(sql.getSql().contains("GROUP BY u.id, u.name"));
        assertTrue(sql.getSql().contains("HAVING COUNT(o.id) > ?"));
        assertTrue(sql.getSql().contains("ORDER BY u.name"));
        assertTrue(sql.getSql().contains("LIMIT 10"));
        assertTrue(sql.getSql().contains("OFFSET 0"));
        assertArrayEquals(new Object[]{"active", 0}, sql.getParams());
    }

    @Test
    @DisplayName("Should build SELECT * with no columns")
    void shouldBuildSelectAllWithNoColumns() {
        BoundSql sql = new SelectBuilder()
            .from("users")
            .build();
        assertEquals("SELECT * FROM users", sql.getSql());
    }
}