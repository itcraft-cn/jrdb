package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.ParamMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyBatisStyleExecutor implements StyleExecutor {

    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{(\\w+)}");

    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(methodMeta, "methodMeta cannot be null");

        String sql = methodMeta.getSql();
        if (sql == null) {
            throw new IllegalArgumentException("SQL is null for method: " + methodMeta.getMethodName());
        }

        String jdbcSql = convertToJdbcSql(sql);
        Object[] params = bindParameters(methodMeta, args);

        SqlOperationType operationType = methodMeta.getOperationType();
        if (operationType == null) {
            throw new IllegalArgumentException("OperationType is null for method: " + methodMeta.getMethodName());
        }

        switch (operationType) {
            case SELECT:
                return executeSelect(session, jdbcSql, params, methodMeta.getReturnType());
            case INSERT:
                return executeInsert(session, jdbcSql, params);
            case UPDATE:
                return executeUpdate(session, jdbcSql, params);
            case DELETE:
                return executeDelete(session, jdbcSql, params);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    public String convertToJdbcSql(String myBatisSql) {
        return myBatisSql.replaceAll("#\\{(\\w+)}", "?");
    }

    private Object[] bindParameters(MethodMeta methodMeta, Object[] args) {
        List<Object> params = new ArrayList<>();
        String sql = methodMeta.getSql();
        
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        int paramIndex = 0;
        while (matcher.find()) {
            if (args != null && paramIndex < args.length) {
                params.add(args[paramIndex]);
                paramIndex++;
            } else {
                throw new IllegalArgumentException("Parameter index out of bounds: " + paramIndex);
            }
        }
        
        return params.toArray();
    }

    private int findArgsIndex(List<ParamMeta> paramMetas, String paramName) {
        for (ParamMeta paramMeta : paramMetas) {
            if (paramMeta.getName().equals(paramName)) {
                return paramMeta.getIndex();
            }
        }
        return -1;
    }

    private Object executeSelect(Session session, String sql, Object[] params, Class<?> returnType) {
        if (returnType == int.class || returnType == Integer.class || returnType == long.class || returnType == Long.class) {
            try (QueryResult rs = session.query(sql, params)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return null;
            }
        } else {
            return session.queryForObject(sql, returnType, params);
        }
    }

    private Object executeInsert(Session session, String sql, Object[] params) {
        return session.insert(sql, params);
    }

    private Object executeUpdate(Session session, String sql, Object[] params) {
        return session.update(sql, params);
    }

    private Object executeDelete(Session session, String sql, Object[] params) {
        return session.delete(sql, params);
    }
}