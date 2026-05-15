package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.util.List;
import java.util.Objects;

public class AutoCrudExecutor implements StyleExecutor {

    private final EntityMapper mapper;
    private final JdbcStyleExecutor jdbcExecutor;

    public AutoCrudExecutor(EntityMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        this.jdbcExecutor = new JdbcStyleExecutor();
    }

    @Override
    public Object execute(Session session, MethodMeta methodMeta, Object[] args) {
        String methodName = methodMeta.getMethodName();

        if ("insert".equals(methodName) || "update".equals(methodName)) {
            Object entity = args[0];
            Object[] entityParams = mapper.mapToRow(entity);
            Object[] mappedParams = mapParams(methodMeta.getParamMapping(), entityParams);
            
            return jdbcExecutor.execute(session, methodMeta, mappedParams);
        } else {
            return jdbcExecutor.execute(session, methodMeta, args);
        }
    }

    private Object[] mapParams(List<Integer> paramMapping, Object[] params) {
        Object[] mappedParams = new Object[paramMapping.size()];
        for (int i = 0; i < paramMapping.size(); i++) {
            mappedParams[i] = params[paramMapping.get(i)];
        }
        return mappedParams;
    }
}