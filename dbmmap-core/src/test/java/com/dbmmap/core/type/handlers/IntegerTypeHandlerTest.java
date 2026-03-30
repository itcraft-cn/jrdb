package com.dbmmap.core.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("IntegerTypeHandler Tests")
class IntegerTypeHandlerTest {

    private final IntegerTypeHandler handler = new IntegerTypeHandler();

    @Test
    @DisplayName("Should get Integer from ResultSet by column name")
    void shouldGetIntegerByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("column")).thenReturn(42);
        when(rs.wasNull()).thenReturn(false);

        Integer result = handler.getResult(rs, "column");
        assertEquals(42, result);
    }

    @Test
    @DisplayName("Should handle null when getting Integer by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("column")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);

        Integer result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get Integer from ResultSet by column index")
    void shouldGetIntegerByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(42);
        when(rs.wasNull()).thenReturn(false);

        Integer result = handler.getResult(rs, 1);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("Should handle null when getting Integer by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);

        Integer result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set Integer parameter")
    void shouldSetIntegerParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, 42);
        verify(ps).setInt(1, 42);
    }

    @Test
    @DisplayName("Should set null for null Integer parameter")
    void shouldSetNullForNullInteger() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.INTEGER);
    }

    @Test
    @DisplayName("Should return Integer class")
    void shouldReturnIntegerClass() {
        assertEquals(Integer.class, handler.getJavaType());
    }
}