package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.session.Session;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DaoProxyTemplate {
    protected final DaoMeta daoMeta;
    protected final Session session;
    protected final StyleExecutor executor;
    protected final Map<Integer, Method> methodNameMap;

    public DaoProxyTemplate(DaoMeta daoMeta, Session session, StyleExecutor executor) {
        this.daoMeta = Objects.requireNonNull(daoMeta, "daoMeta cannot be null");
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.methodNameMap = buildMethodNameMap(daoMeta);
    }

    protected Object executeByHash(int hashCode, Object[] args) {
        Method method = methodNameMap.get(hashCode);
        if (method == null) {
            throw new IllegalArgumentException("Method not found for hashCode: " + hashCode);
        }
        MethodMeta methodMeta = daoMeta.getMethodMeta(method);
        if (methodMeta == null) {
            throw new IllegalArgumentException("MethodMeta not found for method: " + method.getName());
        }
        return executor.execute(session, methodMeta, args);
    }

    protected Map<Integer, Method> getMethodNameMap() {
        return methodNameMap;
    }

    private Map<Integer, Method> buildMethodNameMap(DaoMeta daoMeta) {
        Map<Integer, Method> map = new HashMap<>();
        for (Method method : daoMeta.getDaoInterface().getDeclaredMethods()) {
            map.put(method.getName().hashCode(), method);
        }
        return map;
    }
}