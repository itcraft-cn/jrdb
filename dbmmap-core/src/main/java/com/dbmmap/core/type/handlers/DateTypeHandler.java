package com.dbmmap.core.type.handlers;

import com.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

/**
 * Type handler for Date type.
 * Uses Timestamp for JDBC conversion.
 */
public class DateTypeHandler implements TypeHandler<Date> {

    @Override
    public Date getResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    @Override
    public Date getResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, Date parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(i, new Timestamp(parameter.getTime()));
        }
    }

    @Override
    public Class<Date> getJavaType() {
        return Date.class;
    }
}