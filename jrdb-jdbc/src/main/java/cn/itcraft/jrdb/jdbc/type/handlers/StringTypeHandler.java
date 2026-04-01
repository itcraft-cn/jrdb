package cn.itcraft.jrdb.jdbc.type.handlers;

import cn.itcraft.jrdb.jdbc.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for String type.
 */
public class StringTypeHandler implements TypeHandler<String> {

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
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, parameter);
        }
    }

    @Override
    public Class<String> getJavaType() {
        return String.class;
    }
}