package com.dbmmap.core.jdbc;

import static com.dbmmap.core.util.CheckThrowUtil.ifMatchThrow;

/**
 * @author Helly Guo
 * <p>
 * Created on 3/3/22 10:11 PM
 */
public interface JdbcSqlBox<T> {
    String sqlTemplate();

    int paramSize();

    Object[] convParam(T bean);

    default Object[] fetchParam(T bean, int paramSize) {
        Object[] args = convParam(bean);
        ifMatchThrow(args.length != paramSize, "paramSize is %d, args size is %d, not match", paramSize, args.length);
        return args;
    }
}