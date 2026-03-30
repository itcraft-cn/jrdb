package com.dbmmap.springboot.repository;

import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.Session;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class DbMmapRepositoryFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;
    private Session session;

    public void setInterface(String interfaceName) throws ClassNotFoundException {
        this.interfaceClass = (Class<T>) Class.forName(interfaceName);
    }

    @Autowired
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            new RepositoryInvocationHandler(session)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private static class RepositoryInvocationHandler implements InvocationHandler {
        private final Session session;

        RepositoryInvocationHandler(Session session) {
            this.session = session;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Query query = method.getAnnotation(Query.class);
            if (query != null) {
                String sql = query.value();
                return executeQuery(sql, args, method.getReturnType());
            }

            Update update = method.getAnnotation(Update.class);
            if (update != null) {
                String sql = update.value();
                return session.update(sql, args != null ? args : new Object[0]);
            }

            throw new UnsupportedOperationException("Method must be annotated with @Query or @Update: " + method.getName());
        }

        private Object executeQuery(String sql, Object[] args, Class<?> returnType) {
            Object[] params = args != null ? args : new Object[0];
            
            if (returnType == void.class || returnType == Void.class) {
                try (QueryResult result = session.query(sql, params)) {
                    return null;
                }
            }

            try (QueryResult result = session.query(sql, params)) {
                if (result == null || !result.next()) {
                    return null;
                }

                if (result.getRowCount() == 1) {
                    if (returnType == long.class || returnType == Long.class) {
                        return result.getLong(1);
                    }
                    if (returnType == int.class || returnType == Integer.class) {
                        return result.getInt(1);
                    }
                    if (returnType == String.class) {
                        return result.getString(1);
                    }
                }

                return result;
            }
        }
    }
}