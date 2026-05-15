package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.executor.HibernateStyleExecutor;
import cn.itcraft.jrdb.jdbc.executor.JdbcStyleExecutor;
import cn.itcraft.jrdb.jdbc.executor.MyBatisStyleExecutor;
import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.session.Session;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DaoRegistry {

    private final DaoMetaCache daoMetaCache;
    private final DaoProxyGenerator proxyGenerator;
    private final Map<Class<?>, Object> daoCache = new ConcurrentHashMap<>();
    private final Map<SqlParamBindStyle, StyleExecutor> executorMap;

    public DaoRegistry(MapperRegistry mapperRegistry, SqlParamBindStyle defaultStyle) {
        this.daoMetaCache = new DaoMetaCache(mapperRegistry, defaultStyle);
        this.proxyGenerator = new DaoProxyGenerator();
        this.executorMap = new ConcurrentHashMap<>();
        initExecutors();
    }

    private void initExecutors() {
        executorMap.put(SqlParamBindStyle.MYBATIS, new MyBatisStyleExecutor());
        executorMap.put(SqlParamBindStyle.HIBERNATE, new HibernateStyleExecutor());
        executorMap.put(SqlParamBindStyle.JDBC, new JdbcStyleExecutor());
    }

    public <T> T getDao(Class<T> daoInterface, Session session) {
        Objects.requireNonNull(daoInterface, "daoInterface cannot be null");
        Objects.requireNonNull(session, "session cannot be null");

        DaoMeta daoMeta = daoMetaCache.getDaoMeta(daoInterface);
        StyleExecutor executor = getExecutor(daoMeta);

        return proxyGenerator.generateProxy(daoInterface, daoMeta, executor, session);
    }

    private StyleExecutor getExecutor(DaoMeta daoMeta) {
        SqlParamBindStyle style = daoMeta.getClassStyle();
        StyleExecutor executor = executorMap.get(style);
        if (executor == null) {
            throw new IllegalArgumentException("Unknown style: " + style);
        }
        return executor;
    }

    public DaoMetaCache getDaoMetaCache() {
        return daoMetaCache;
    }

    public void clear() {
        daoMetaCache.clear();
        daoCache.clear();
    }
}