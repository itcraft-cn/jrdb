package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.List;
import java.util.Objects;

/**
 * 抽象执行器基类，提供SQL操作的通用执行逻辑。
 *
 * @author Helly Guo
 * @since 1.2.0
 */
public abstract class AbstractStyleExecutor implements StyleExecutor {

    /**
     * 执行SQL操作。
     *
     * @param session 会话对象
     * @param methodMeta 方法元数据
     * @param args 参数数组
     * @return 执行结果
     */
    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(methodMeta, "methodMeta cannot be null");

        String sql = methodMeta.getCachedSql();
        if (sql == null) {
            throw new IllegalArgumentException("Cached SQL is null for method: " + methodMeta.getMethodName());
        }

        Object[] params = bindParametersFromCache(methodMeta, args);

        SqlOperationType operationType = methodMeta.getOperationType();
        if (operationType == null) {
            throw new IllegalArgumentException("OperationType is null for method: " + methodMeta.getMethodName());
        }

        switch (operationType) {
            case SELECT:
                return executeSelect(session, sql, params, methodMeta.getReturnType());
            case INSERT:
                return executeInsert(session, sql, params);
            case UPDATE:
                return executeUpdate(session, sql, params);
            case DELETE:
                return executeDelete(session, sql, params);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    /**
     * 从缓存绑定参数。
     *
     * @param methodMeta 方法元数据
     * @param args 参数数组
     * @return 绑定后的参数数组
     */
    protected Object[] bindParametersFromCache(MethodMeta methodMeta, Object[] args) {
        List<Integer> paramMapping = methodMeta.getParamMapping();
        Object[] params = new Object[paramMapping.size()];

        for (int i = 0; i < paramMapping.size(); i++) {
            int argsIndex = paramMapping.get(i);
            if (args != null && argsIndex < args.length) {
                params[i] = args[argsIndex];
            } else {
                throw new IllegalArgumentException("Parameter index out of bounds: " + argsIndex);
            }
        }

        return params;
    }

    /**
     * 执行查询操作。
     *
     * @param session 会话对象
     * @param sql SQL语句
     * @param params 参数数组
     * @param returnType 返回类型
     * @return 查询结果
     */
    protected Object executeSelect(Session session, String sql, Object[] params, Class<?> returnType) {
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

    /**
     * 执行插入操作。
     *
     * @param session 会话对象
     * @param sql SQL语句
     * @param params 参数数组
     * @return 影响的行数
     */
    protected Object executeInsert(Session session, String sql, Object[] params) {
        boolean result = session.insert(sql, params);
        return result ? 1 : 0;
    }

    /**
     * 执行更新操作。
     *
     * @param session 会话对象
     * @param sql SQL语句
     * @param params 参数数组
     * @return 影响的行数
     */
    protected Object executeUpdate(Session session, String sql, Object[] params) {
        return session.update(sql, params);
    }

    /**
     * 执行删除操作。
     *
     * @param session 会话对象
     * @param sql SQL语句
     * @param params 参数数组
     * @return 影响的行数
     */
    protected Object executeDelete(Session session, String sql, Object[] params) {
        return session.delete(sql, params);
    }
}
