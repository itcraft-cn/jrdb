package cn.itcraft.jrdb.jdbc.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("BooleanTypeHandler Tests")
class BooleanTypeHandlerTest {

    private final BooleanTypeHandler handler = new BooleanTypeHandler();

    @Test
    @DisplayName("Should get Boolean from ResultSet by column name")
    void shouldGetBooleanByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("column")).thenReturn(true);
        when(rs.wasNull()).thenReturn(false);

        Boolean result = handler.getResult(rs, "column");
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle null when getting Boolean by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("column")).thenReturn(false);
        when(rs.wasNull()).thenReturn(true);

        Boolean result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get Boolean from ResultSet by column index")
    void shouldGetBooleanByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean(1)).thenReturn(true);
        when(rs.wasNull()).thenReturn(false);

        Boolean result = handler.getResult(rs, 1);
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle null when getting Boolean by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean(1)).thenReturn(false);
        when(rs.wasNull()).thenReturn(true);

        Boolean result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set Boolean parameter")
    void shouldSetBooleanParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, true);
        verify(ps).setBoolean(1, true);
    }

    @Test
    @DisplayName("Should set null for null Boolean parameter")
    void shouldSetNullForNullBoolean() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.BOOLEAN);
    }

    @Test
    @DisplayName("Should return Boolean class")
    void shouldReturnBooleanClass() {
        assertEquals(Boolean.class, handler.getJavaType());
    }
}