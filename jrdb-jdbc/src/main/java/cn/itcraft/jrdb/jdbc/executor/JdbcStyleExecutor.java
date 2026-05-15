package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.Objects;

public class JdbcStyleExecutor implements StyleExecutor {

    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(methodMeta, "methodMeta cannot be null");

        String sql = methodMeta.getSql();
        if (sql == null) {
            throw new IllegalArgumentException("SQL is null for method: " + methodMeta.getMethodName());
        }

        SqlOperationType operationType = methodMeta.getOperationType();
        if (operationType == null) {
            throw new IllegalArgumentException("OperationType is null for method: " + methodMeta.getMethodName());
        }

        switch (operationType) {
            case SELECT:
                return executeSelect(session, sql, args, methodMeta.getReturnType());
            case INSERT:
                return executeInsert(session, sql, args);
            case UPDATE:
                return executeUpdate(session, sql, args);
            case DELETE:
                return executeDelete(session, sql, args);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    private Object executeSelect(Session session, String sql, Object[] args, Class<?> returnType) {
        if (returnType == int.class || returnType == Integer.class || returnType == long.class || returnType == Long.class) {
            try (QueryResult rs = session.query(sql, args)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return null;
            }
        } else {
            return session.queryForObject(sql, returnType, args);
        }
    }

    private Object executeInsert(Session session, String sql, Object[] args) {
        return session.insert(sql, args);
    }

    private Object executeUpdate(Session session, String sql, Object[] args) {
        return session.update(sql, args);
    }

    private Object executeDelete(Session session, String sql, Object[] args) {
        return session.delete(sql, args);
    }
}