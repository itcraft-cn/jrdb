package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.session.Session;

public interface StyleExecutor {
    Object execute(Session session, MethodMeta methodMeta, Object[] args);
}