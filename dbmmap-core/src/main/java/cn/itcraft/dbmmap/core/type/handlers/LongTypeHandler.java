package cn.itcraft.dbmmap.core.type.handlers;

import cn.itcraft.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for Long type.
 */
public class LongTypeHandler implements TypeHandler<Long> {

    @Override
    public Long getResult(ResultSet rs, String columnName) throws SQLException {
        long result = rs.getLong(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
        long result = rs.getLong(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Long parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.BIGINT);
        } else {
            ps.setLong(i, parameter);
        }
    }

    @Override
    public Class<Long> getJavaType() {
        return Long.class;
    }
}