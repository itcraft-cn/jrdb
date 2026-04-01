package cn.itcraft.jrdb.jdbc.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

@DisplayName("DateTypeHandler Tests")
class DateTypeHandlerTest {

    private final DateTypeHandler handler = new DateTypeHandler();

    @Test
    @DisplayName("Should get Date from ResultSet by column name")
    void shouldGetDateByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp ts = new Timestamp(1700000000000L);
        when(rs.getTimestamp("column")).thenReturn(ts);

        Date result = handler.getResult(rs, "column");
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    @DisplayName("Should handle null when getting Date by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("column")).thenReturn(null);

        Date result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get Date from ResultSet by column index")
    void shouldGetDateByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp ts = new Timestamp(1700000000000L);
        when(rs.getTimestamp(1)).thenReturn(ts);

        Date result = handler.getResult(rs, 1);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    @DisplayName("Should handle null when getting Date by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        Date result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set Date parameter")
    void shouldSetDateParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        Date date = new Date(1700000000000L);

        handler.setParameter(ps, 1, date);
        verify(ps).setTimestamp(1, new Timestamp(date.getTime()));
    }

    @Test
    @DisplayName("Should set null for null Date parameter")
    void shouldSetNullForNullDate() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.TIMESTAMP);
    }

    @Test
    @DisplayName("Should return Date class")
    void shouldReturnDateClass() {
        assertEquals(Date.class, handler.getJavaType());
    }
}