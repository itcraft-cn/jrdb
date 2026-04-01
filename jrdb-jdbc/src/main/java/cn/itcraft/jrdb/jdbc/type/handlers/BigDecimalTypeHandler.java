package cn.itcraft.jrdb.jdbc.type.handlers;

import cn.itcraft.jrdb.jdbc.type.TypeHandler;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for BigDecimal type.
 */
public class BigDecimalTypeHandler implements TypeHandler<BigDecimal> {

    @Override
    public BigDecimal getResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBigDecimal(columnName);
    }

    @Override
    public BigDecimal getResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, BigDecimal parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.DECIMAL);
        } else {
            ps.setBigDecimal(i, parameter);
        }
    }

    @Override
    public Class<BigDecimal> getJavaType() {
        return BigDecimal.class;
    }
}