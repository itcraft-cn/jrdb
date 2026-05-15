package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.session.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DaoProxyGenerator {

    public <T> T generateProxy(Class<T> daoInterface, DaoMeta daoMeta, StyleExecutor executor, Session session) {
        DaoInvocationHandler handler = new DaoInvocationHandler(daoMeta, executor, session);
        return daoInterface.cast(Proxy.newProxyInstance(
                daoInterface.getClassLoader(),
                new Class[]{daoInterface},
                handler
        ));
    }

    private static class DaoInvocationHandler implements InvocationHandler {
        private final DaoMeta daoMeta;
        private final StyleExecutor executor;
        private final Session session;

        DaoInvocationHandler(DaoMeta daoMeta, StyleExecutor executor, Session session) {
            this.daoMeta = daoMeta;
            this.executor = executor;
            this.session = session;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodMeta methodMeta = daoMeta.getMethodMeta(method);
            if (methodMeta == null) {
                throw new IllegalArgumentException("Method not found in daoMeta: " + method.getName());
            }

            return executor.execute(session, methodMeta, args);
        }
    }
}