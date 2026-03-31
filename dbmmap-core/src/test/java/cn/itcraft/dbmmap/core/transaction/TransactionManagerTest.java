package cn.itcraft.dbmmap.core.transaction;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionManagerTest {

    @Test
    void shouldBeginTransaction() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        TransactionManager manager = new TransactionManager(dataSource);
        Transaction tx = manager.beginTransaction();

        assertNotNull(tx);
        assertFalse(tx.isClosed());
    }

    @Test
    void shouldExecuteInTransaction() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        TransactionManager manager = new TransactionManager(dataSource);

        String result = manager.executeInTransaction(tx -> {
            try {
                assertNotNull(tx.getConnection());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return "success";
        });

        assertEquals("success", result);
        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void shouldRollbackOnException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        TransactionManager manager = new TransactionManager(dataSource);

        SQLException ex = assertThrows(SQLException.class, () -> {
            manager.executeInTransaction(tx -> {
                try {
                    tx.getConnection();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                throw new RuntimeException("test error");
            });
        });
        assertTrue(ex.getCause() instanceof RuntimeException);

        verify(connection).rollback();
        verify(connection).close();
    }

    @Test
    void shouldRunInTransaction() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        TransactionManager manager = new TransactionManager(dataSource);
        int[] counter = new int[]{0};

        manager.runInTransaction(tx -> {
            counter[0]++;
            assertNotNull(tx.getConnection());
        });

        assertEquals(1, counter[0]);
        verify(connection).commit();
    }

    @Test
    void shouldThrowOnNullDataSource() {
        assertThrows(NullPointerException.class, () -> new TransactionManager(null));
    }
}