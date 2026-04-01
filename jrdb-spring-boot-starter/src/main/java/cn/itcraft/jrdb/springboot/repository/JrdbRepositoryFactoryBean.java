package cn.itcraft.jrdb.springboot.repository;

import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

public class JrdbRepositoryFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;
    private Session session;
    private MapperRegistry mapperRegistry;

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

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[] { interfaceClass },
            new RepositoryInvocationHandler(session, mapperRegistry)
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

        RepositoryInvocationHandler(Session session, MapperRegistry mapperRegistry) {
            this.session = session;
            this.mapperRegistry = mapperRegistry;
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
            boolean isList = genericReturnType instanceof ParameterizedType 
                && ((ParameterizedType) genericReturnType).getRawType() == List.class;

            if (isList) {
                return session.queryForList(sql, entityClass, params);
            } else {
                return session.queryForObject(sql, entityClass, params);
            }
        }
    }
}