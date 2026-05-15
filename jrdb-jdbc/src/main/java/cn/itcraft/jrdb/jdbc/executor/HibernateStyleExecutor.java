package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.Objects;

public class HibernateStyleExecutor implements StyleExecutor {

    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(methodMeta, "methodMeta cannot be null");

        String hql = methodMeta.getSql();
        if (hql == null) {
            throw new IllegalArgumentException("HQL is null for method: " + methodMeta.getMethodName());
        }

        String sql = convertHqlToSql(hql);

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

    public String convertHqlToSql(String hql) {
        if (hql.toLowerCase().startsWith("from ")) {
            String entityPart = hql.substring(5).trim();
            int whereIndex = entityPart.toLowerCase().indexOf(" where ");
            if (whereIndex > 0) {
                String entityName = entityPart.substring(0, whereIndex).trim();
                String whereClause = entityPart.substring(whereIndex + 7).trim();
                return "SELECT * FROM " + inferTableName(entityName) + " WHERE " + whereClause;
            } else {
                String entityName = entityPart;
                return "SELECT * FROM " + inferTableName(entityName);
            }
        }
        return hql;
    }

    private String inferTableName(String entityName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < entityName.length(); i++) {
            char c = entityName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
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