package cn.itcraft.jrdb.jdbc.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoCrudMeta Test")
class AutoCrudMetaTest {

    @Test
    @DisplayName("constructor with null entityClass should throw NullPointerException")
    void constructorNullEntityClass() {
        assertThrows(NullPointerException.class, () -> new AutoCrudMeta(null, "id", "users"));
    }

    @Test
    @DisplayName("constructor with null primaryKey should throw NullPointerException")
    void constructorNullPrimaryKey() {
        assertThrows(NullPointerException.class, () -> new AutoCrudMeta(User.class, null, "users"));
    }

    @Test
    @DisplayName("constructor with null tableName should throw NullPointerException")
    void constructorNullTableName() {
        assertThrows(NullPointerException.class, () -> new AutoCrudMeta(User.class, "id", null));
    }

    @Test
    @DisplayName("constructor with valid parameters should succeed")
    void constructorValid() {
        AutoCrudMeta meta = new AutoCrudMeta(User.class, "id", "users");
        assertEquals(User.class, meta.getEntityClass());
        assertEquals("id", meta.getPrimaryKey());
        assertEquals("users", meta.getTableName());
    }

    static class User {
        private Long id;
        private String name;
    }
}