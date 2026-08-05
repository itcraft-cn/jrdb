package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.session.Session;

/**
 * 执行器接口，定义不同风格的SQL执行策略。
 *
 * @author Helly Guo
 * @since 1.2.0
 */
public interface StyleExecutor {
    /**
     * 执行SQL操作。
     *
     * @param session 会话对象
     * @param methodMeta 方法元数据
     * @param args 参数数组
     * @return 执行结果
     */
    Object execute(Session session, MethodMeta methodMeta, Object[] args);
}
