package cn.itcraft.jrdb.jdbc.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParamMeta Test")
class ParamMetaTest {

    @Test
    @DisplayName("constructor should work")
    void constructor() {
        ParamMeta meta = new ParamMeta(0, "id", Long.class);
        assertEquals(0, meta.getIndex());
        assertEquals("id", meta.getName());
        assertEquals(Long.class, meta.getType());
    }

    @Test
    @DisplayName("constructor with different index")
    void constructorDifferentIndex() {
        ParamMeta meta = new ParamMeta(1, "name", String.class);
        assertEquals(1, meta.getIndex());
        assertEquals("name", meta.getName());
        assertEquals(String.class, meta.getType());
    }
}