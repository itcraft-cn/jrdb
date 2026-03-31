package com.dbmmap.core.session;

import com.dbmmap.core.handler.RowHandler;
import com.dbmmap.core.memory.MemoryPool;
import com.dbmmap.core.query.QueryResult;

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

    MemoryPool getMemoryPool();

    boolean isClosed();

    void close();
}