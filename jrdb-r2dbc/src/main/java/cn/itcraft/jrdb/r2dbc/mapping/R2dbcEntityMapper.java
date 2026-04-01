package cn.itcraft.jrdb.r2dbc.mapping;

import io.r2dbc.spi.Row;

public interface R2dbcEntityMapper<T> {

    T mapFromRow(Row row);

    Object[] mapToRow(T entity);
}