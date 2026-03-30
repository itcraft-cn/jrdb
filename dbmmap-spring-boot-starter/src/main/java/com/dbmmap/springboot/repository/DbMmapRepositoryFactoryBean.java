package com.dbmmap.springboot.repository;

import com.dbmmap.core.mapping.EntityMapper;
import com.dbmmap.core.mapping.MapperRegistry;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.Session;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DbMmapRepositoryFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;
    private Session session;
    private MapperRegistry mapperRegistry;
    private DataSource dataSource;

    public void setInterface(String interfaceName) throws ClassNotFoundException {
        this.interfaceClass = (Class<T>) Class.forName(interfaceName);
    }

    @Autowired
    public void setSession(Session session) {
        this.session = session;
    }

    @Autowired
    public void setMapperRegistry(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            new RepositoryInvocationHandler(session, mapperRegistry, dataSource)
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
        private final MapperRegistry mapperRegistry;
        private final DataSource dataSource;

        RepositoryInvocationHandler(Session session, MapperRegistry mapperRegistry, DataSource dataSource) {
            this.session = session;
            this.mapperRegistry = mapperRegistry;
            this.dataSource = dataSource;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Query query = method.getAnnotation(Query.class);
            if (query != null) {
                String sql = query.value();
                return executeQuery(sql, args, method.getGenericReturnType(), method.getReturnType());
            }

            Update update = method.getAnnotation(Update.class);
            if (update != null) {
                String sql = update.value();
                return session.update(sql, args != null ? args : new Object[0]);
            }

            throw new UnsupportedOperationException("Method must be annotated with @Query or @Update: " + method.getName());
        }

        private Object executeQuery(String sql, Object[] args, Type genericReturnType, Class<?> returnType) {
            Object[] params = args != null ? args : new Object[0];

            if (returnType == void.class || returnType == Void.class) {
                try (QueryResult result = session.query(sql, params)) {
                    return null;
                }
            }

            Class<?> entityClass = getEntityClass(genericReturnType, returnType);
            if (entityClass != null) {
                return executeEntityQuery(sql, params, genericReturnType, entityClass);
            }

            try (QueryResult result = session.query(sql, params)) {
                if (result == null || !result.next()) {
                    return null;
                }
                if (returnType == long.class || returnType == Long.class) {
                    return result.getLong(1);
                }
                if (returnType == int.class || returnType == Integer.class) {
                    return result.getInt(1);
                }
                if (returnType == String.class) {
                    return result.getString(1);
                }
                return result;
            }
        }

        private Class<?> getEntityClass(Type genericReturnType, Class<?> returnType) {
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericReturnType;
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
            if (!returnType.isPrimitive() && !returnType.getName().startsWith("java.")) {
                return returnType;
            }
            return null;
        }

        private Object executeEntityQuery(String sql, Object[] params, Type genericReturnType, Class<?> entityClass) {
            EntityMapper<?> mapper = mapperRegistry.getMapper(entityClass);
            boolean isList = genericReturnType instanceof ParameterizedType 
                && ((ParameterizedType) genericReturnType).getRawType() == List.class;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (isList) {
                        List<Object> list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(mapper.mapFromResultSet(rs));
                        }
                        return list;
                    } else {
                        if (rs.next()) {
                            return mapper.mapFromResultSet(rs);
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Query failed: " + sql, e);
            }
        }
    }
}