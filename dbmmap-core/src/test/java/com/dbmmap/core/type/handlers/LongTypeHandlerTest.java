package com.dbmmap.core.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("LongTypeHandler Tests")
class LongTypeHandlerTest {

    private final LongTypeHandler handler = new LongTypeHandler();

    @Test
    @DisplayName("Should get Long from ResultSet by column name")
    void shouldGetLongByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("column")).thenReturn(42L);
        when(rs.wasNull()).thenReturn(false);

        Long result = handler.getResult(rs, "column");
        assertEquals(42L, result);
    }

    @Test
    @DisplayName("Should handle null when getting Long by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("column")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);

        Long result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get Long from ResultSet by column index")
    void shouldGetLongByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(42L);
        when(rs.wasNull()).thenReturn(false);

        Long result = handler.getResult(rs, 1);
        assertEquals(42L, result);
    }

    @Test
    @DisplayName("Should handle null when getting Long by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);

        Long result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set Long parameter")
    void shouldSetLongParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, 42L);
        verify(ps).setLong(1, 42L);
    }

    @Test
    @DisplayName("Should set null for null Long parameter")
    void shouldSetNullForNullLong() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.BIGINT);
    }

    @Test
    @DisplayName("Should return Long class")
    void shouldReturnLongClass() {
        assertEquals(Long.class, handler.getJavaType());
    }
}