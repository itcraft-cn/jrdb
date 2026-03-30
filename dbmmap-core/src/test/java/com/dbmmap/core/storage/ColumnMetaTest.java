package com.dbmmap.core.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColumnMetaTest {

    @Test
    void shouldCreateColumnMeta() {
        ColumnMeta meta = new ColumnMeta("id", java.sql.Types.INTEGER, 4);
        assertEquals("id", meta.getName());
        assertEquals(java.sql.Types.INTEGER, meta.getSqlType());
        assertEquals(4, meta.getSize());
    }

    @Test
    void shouldCreateStringColumn() {
        ColumnMeta meta = new ColumnMeta("name", java.sql.Types.VARCHAR, 100);
        assertEquals("name", meta.getName());
        assertEquals(java.sql.Types.VARCHAR, meta.getSqlType());
        assertEquals(100, meta.getSize());
    }
}