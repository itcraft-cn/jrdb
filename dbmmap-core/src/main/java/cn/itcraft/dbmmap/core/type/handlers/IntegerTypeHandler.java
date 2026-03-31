package cn.itcraft.dbmmap.core.type.handlers;

import cn.itcraft.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for Integer type.
 */
public class IntegerTypeHandler implements TypeHandler<Integer> {

    @Override
    public Integer getResult(ResultSet rs, String columnName) throws SQLException {
        int result = rs.getInt(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
        int result = rs.getInt(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Integer parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, parameter);
        }
    }

    @Override
    public Class<Integer> getJavaType() {
        return Integer.class;
    }
}