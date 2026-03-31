package cn.itcraft.dbmmap.core.type.handlers;

import cn.itcraft.dbmmap.core.type.TypeHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Type handler for Enum types.
 * Stores enums as String (name).
 *
 * @param <E> the enum type
 */
public class EnumTypeHandler<E extends Enum<E>> implements TypeHandler<E> {

    private final Class<E> enumClass;

    /**
     * Constructs an EnumTypeHandler for the specified enum class.
     *
     * @param enumClass the enum class
     */
    public EnumTypeHandler(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public E getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? Enum.valueOf(enumClass, value) : null;
    }

    @Override
    public E getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? Enum.valueOf(enumClass, value) : null;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, E parameter) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, parameter.name());
        }
    }

    @Override
    public Class<E> getJavaType() {
        return enumClass;
    }
}