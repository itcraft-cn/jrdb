package cn.itcraft.dbmmap.core.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("StringTypeHandler Tests")
class StringTypeHandlerTest {

    private final StringTypeHandler handler = new StringTypeHandler();

    @Test
    @DisplayName("Should get String from ResultSet by column name")
    void shouldGetStringByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        String result = handler.getResult(rs, "column");
        assertEquals("test", result);
    }

    @Test
    @DisplayName("Should handle null when getting String by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn(null);

        String result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get String from ResultSet by column index")
    void shouldGetStringByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        String result = handler.getResult(rs, 1);
        assertEquals("test", result);
    }

    @Test
    @DisplayName("Should handle null when getting String by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        String result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set String parameter")
    void shouldSetStringParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, "test");
        verify(ps).setString(1, "test");
    }

    @Test
    @DisplayName("Should set null for null String parameter")
    void shouldSetNullForNullString() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.VARCHAR);
    }

    @Test
    @DisplayName("Should return String class")
    void shouldReturnStringClass() {
        assertEquals(String.class, handler.getJavaType());
    }
}