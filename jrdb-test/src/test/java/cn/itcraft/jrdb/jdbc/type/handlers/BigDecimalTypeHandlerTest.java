package cn.itcraft.jrdb.jdbc.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("BigDecimalTypeHandler Tests")
class BigDecimalTypeHandlerTest {

    private final BigDecimalTypeHandler handler = new BigDecimalTypeHandler();

    @Test
    @DisplayName("Should get BigDecimal from ResultSet by column name")
    void shouldGetBigDecimalByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        BigDecimal expected = new BigDecimal("123.45");
        when(rs.getBigDecimal("column")).thenReturn(expected);

        BigDecimal result = handler.getResult(rs, "column");
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle null when getting BigDecimal by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBigDecimal("column")).thenReturn(null);

        BigDecimal result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get BigDecimal from ResultSet by column index")
    void shouldGetBigDecimalByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        BigDecimal expected = new BigDecimal("123.45");
        when(rs.getBigDecimal(1)).thenReturn(expected);

        BigDecimal result = handler.getResult(rs, 1);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle null when getting BigDecimal by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBigDecimal(1)).thenReturn(null);

        BigDecimal result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set BigDecimal parameter")
    void shouldSetBigDecimalParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        BigDecimal value = new BigDecimal("123.45");

        handler.setParameter(ps, 1, value);
        verify(ps).setBigDecimal(1, value);
    }

    @Test
    @DisplayName("Should set null for null BigDecimal parameter")
    void shouldSetNullForNullBigDecimal() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.DECIMAL);
    }

    @Test
    @DisplayName("Should return BigDecimal class")
    void shouldReturnBigDecimalClass() {
        assertEquals(BigDecimal.class, handler.getJavaType());
    }
}