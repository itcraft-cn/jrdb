package cn.itcraft.dbmmap.core.type.handlers;

import cn.itcraft.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for Boolean type.
 */
public class BooleanTypeHandler implements TypeHandler<Boolean> {

    @Override
    public Boolean getResult(ResultSet rs, String columnName) throws SQLException {
        boolean result = rs.getBoolean(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    public Boolean getResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean result = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Boolean parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.BOOLEAN);
        } else {
            ps.setBoolean(i, parameter);
        }
    }

    @Override
    public Class<Boolean> getJavaType() {
        return Boolean.class;
    }
}