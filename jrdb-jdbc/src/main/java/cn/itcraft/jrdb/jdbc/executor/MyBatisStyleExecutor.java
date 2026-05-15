package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.ParamMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
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
                return executeSelect(session, jdbcSql, params);
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

    private String convertToJdbcSql(String myBatisSql) {
        return myBatisSql.replaceAll("#\\{(\\w+)}", "?");
    }

    private Object[] bindParameters(MethodMeta methodMeta, Object[] args) {
        List<Object> params = new ArrayList<>();
        List<ParamMeta> paramMetas = methodMeta.getParamMetas();

        for (ParamMeta paramMeta : paramMetas) {
            String name = paramMeta.getName();
            int index = findParamIndex(methodMeta.getSql(), name);
            if (index >= 0 && args != null && index < args.length) {
                params.add(args[index]);
            }
        }

        return params.toArray();
    }

    private int findParamIndex(String sql, String paramName) {
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        int count = 0;
        while (matcher.find()) {
            if (matcher.group(1).equals(paramName)) {
                return count;
            }
            count++;
        }
        return -1;
    }

    private Object executeSelect(Session session, String sql, Object[] params) {
        return session.query(sql, rs -> {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return null;
        });
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