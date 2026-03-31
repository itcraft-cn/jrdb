package cn.itcraft.jrdb.core.executor;

import cn.itcraft.jrdb.core.config.JrdbConfig;
import cn.itcraft.jrdb.core.memory.MemoryPool;
import cn.itcraft.jrdb.core.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.core.query.QueryResult;
import cn.itcraft.jrdb.core.sql.BoundSql;
import cn.itcraft.jrdb.core.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JdbcQueryExecutorTest {

    private MemoryPool pool;
    private JrdbConfig config;
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metaData;

    @BeforeEach
    void setUp() {
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
                .preAllocMemSize(1024 * 1024)
                .queryCacheSize(64 * 1024)
                .build();
        pool = new MemoryPool(poolConfig);
        config = JrdbConfig.builder().memoryPoolConfig(poolConfig).build();
        
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        metaData = mock(ResultSetMetaData.class);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.destroy();
        }
    }

    @Test
    void shouldExecuteQuery() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(metaData.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(metaData.getColumnDisplaySize(1)).thenReturn(10);
        when(metaData.getColumnDisplaySize(2)).thenReturn(50);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt(1)).thenReturn(1, 2);
        when(resultSet.getString(2)).thenReturn("Alice", "Bob");

        JdbcQueryExecutor executor = new JdbcQueryExecutor(pool, config);
        Transaction tx = mock(Transaction.class);
        when(tx.getConnection()).thenReturn(connection);
        executor.setTransaction(tx);

        BoundSql boundSql = new BoundSql("SELECT id, name FROM users", null);
        QueryResult result = executor.query(boundSql);

        assertNotNull(result);
        assertEquals(2, result.getRowCount());
        assertEquals(2, result.getColumnCount());
        assertEquals("id", result.getColumnName(0));
        assertEquals("name", result.getColumnName(1));

        assertTrue(result.next());
        assertEquals(1, result.getInt(0));
        assertEquals("Alice", result.getString(1));

        assertTrue(result.next());
        assertEquals(2, result.getInt(0));
        assertEquals("Bob", result.getString(1));

        assertFalse(result.next());
        result.close();
    }

    @Test
    void shouldExecuteUpdate() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(5);

        JdbcQueryExecutor executor = new JdbcQueryExecutor(pool, config);
        Transaction tx = mock(Transaction.class);
        when(tx.getConnection()).thenReturn(connection);
        executor.setTransaction(tx);

        BoundSql boundSql = new BoundSql("UPDATE users SET active = 1", null);
        int affected = executor.update(boundSql);

        assertEquals(5, affected);
    }

    @Test
    void shouldHandleParameters() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnName(1)).thenReturn("count");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(metaData.getColumnDisplaySize(1)).thenReturn(10);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt(1)).thenReturn(42);

        JdbcQueryExecutor executor = new JdbcQueryExecutor(pool, config);
        Transaction tx = mock(Transaction.class);
        when(tx.getConnection()).thenReturn(connection);
        executor.setTransaction(tx);

        BoundSql boundSql = new BoundSql("SELECT COUNT(*) FROM users WHERE id > ?", new Object[]{10});
        QueryResult result = executor.query(boundSql);

        assertNotNull(result);
        verify(statement).setObject(1, 10);
        result.close();
    }

    @Test
    void shouldLimitRows() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(metaData.getColumnDisplaySize(1)).thenReturn(10);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getInt(1)).thenReturn(1, 2, 3);

        JdbcQueryExecutor executor = new JdbcQueryExecutor(pool, config);
        executor.setMaxRows(2);
        Transaction tx = mock(Transaction.class);
        when(tx.getConnection()).thenReturn(connection);
        executor.setTransaction(tx);

        BoundSql boundSql = new BoundSql("SELECT id FROM users", null);
        QueryResult result = executor.query(boundSql);

        assertEquals(2, result.getRowCount());
        result.close();
    }

    @Test
    void shouldThrowOnNullPool() {
        assertThrows(NullPointerException.class, () -> new JdbcQueryExecutor(null, config));
    }

    @Test
    void shouldThrowOnNullConfig() {
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder().build();
        MemoryPool localPool = new MemoryPool(poolConfig);
        assertThrows(NullPointerException.class, () -> new JdbcQueryExecutor(localPool, null));
        localPool.destroy();
    }
}