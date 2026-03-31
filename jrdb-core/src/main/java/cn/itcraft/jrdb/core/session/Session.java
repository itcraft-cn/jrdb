package cn.itcraft.jrdb.core.session;

import cn.itcraft.jrdb.core.handler.RowHandler;
import cn.itcraft.jrdb.core.mapping.EntityMapper;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.query.QueryResult;

import java.util.List;
import java.util.function.Function;

public interface Session extends AutoCloseable {

    QueryResult query(String sql);

    QueryResult query(String sql, Object... params);

    <T> T query(String sql, Function<QueryResult, T> handler);

    void queryWithHandler(String sql, RowHandler handler);

    void queryWithHandler(String sql, RowHandler handler, Object... params);

    int update(String sql);

    int update(String sql, Object... params);

    int[] batchUpdate(String... sqls);

    int[] batchExecute(String sql, List<Object[]> paramsList);

    <T> int[] batchExecute(String sql, List<T> entities, EntityMapper<T> mapper);

    MemoryPool getMemoryPool();

    boolean isClosed();

    void close();
}