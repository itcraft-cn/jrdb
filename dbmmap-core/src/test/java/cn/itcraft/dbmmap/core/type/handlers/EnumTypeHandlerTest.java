package cn.itcraft.dbmmap.core.type.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@DisplayName("EnumTypeHandler Tests")
class EnumTypeHandlerTest {

    enum TestEnum {
        VALUE_A, VALUE_B, VALUE_C
    }

    private final EnumTypeHandler<TestEnum> handler = new EnumTypeHandler<>(TestEnum.class);

    @Test
    @DisplayName("Should get Enum from ResultSet by column name")
    void shouldGetEnumByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("VALUE_A");

        TestEnum result = handler.getResult(rs, "column");
        assertEquals(TestEnum.VALUE_A, result);
    }

    @Test
    @DisplayName("Should handle null when getting Enum by column name")
    void shouldHandleNullByColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn(null);

        TestEnum result = handler.getResult(rs, "column");
        assertNull(result);
    }

    @Test
    @DisplayName("Should get Enum from ResultSet by column index")
    void shouldGetEnumByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("VALUE_B");

        TestEnum result = handler.getResult(rs, 1);
        assertEquals(TestEnum.VALUE_B, result);
    }

    @Test
    @DisplayName("Should handle null when getting Enum by column index")
    void shouldHandleNullByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        TestEnum result = handler.getResult(rs, 1);
        assertNull(result);
    }

    @Test
    @DisplayName("Should set Enum parameter")
    void shouldSetEnumParameter() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, TestEnum.VALUE_C);
        verify(ps).setString(1, "VALUE_C");
    }

    @Test
    @DisplayName("Should set null for null Enum parameter")
    void shouldSetNullForNullEnum() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setParameter(ps, 1, null);
        verify(ps).setNull(1, Types.VARCHAR);
    }

    @Test
    @DisplayName("Should return Enum class")
    void shouldReturnEnumClass() {
        assertEquals(TestEnum.class, handler.getJavaType());
    }
}