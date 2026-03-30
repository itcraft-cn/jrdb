package com.dbmmap.core.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@DisplayName("TypeHandler Interface Tests")
class TypeHandlerTest {

    @Test
    @DisplayName("TypeHandler should define getResult with column name")
    void shouldDefineGetResultWithColumnName() throws SQLException {
        TypeHandler<String> handler = new TestStringHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("test_column")).thenReturn("test_value");
        
        String result = handler.getResult(rs, "test_column");
        assertEquals("test_value", result);
    }

    @Test
    @DisplayName("TypeHandler should define getResult with column index")
    void shouldDefineGetResultWithColumnIndex() throws SQLException {
        TypeHandler<String> handler = new TestStringHandler();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test_value");
        
        String result = handler.getResult(rs, 1);
        assertEquals("test_value", result);
    }

    @Test
    @DisplayName("TypeHandler should define setParameter")
    void shouldDefineSetParameter() throws SQLException {
        TypeHandler<String> handler = new TestStringHandler();
        PreparedStatement ps = mock(PreparedStatement.class);
        
        handler.setParameter(ps, 1, "value");
        verify(ps).setString(1, "value");
    }

    @Test
    @DisplayName("TypeHandler should return Java type")
    void shouldReturnJavaType() {
        TypeHandler<String> handler = new TestStringHandler();
        assertEquals(String.class, handler.getJavaType());
    }

    private static class TestStringHandler implements TypeHandler<String> {
        @Override
        public String getResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getString(columnName);
        }

        @Override
        public String getResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getString(columnIndex);
        }

        @Override
        public void setParameter(PreparedStatement ps, int i, String parameter) throws SQLException {
            if (parameter == null) {
                ps.setNull(i, java.sql.Types.VARCHAR);
            } else {
                ps.setString(i, parameter);
            }
        }

        @Override
        public Class<String> getJavaType() {
            return String.class;
        }
    }
}