package cn.itcraft.jrdb.core.transaction;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionTest {

    @Test
    void shouldGetConnection() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        Connection conn = tx.getConnection();

        assertNotNull(conn);
        assertEquals(connection, conn);
    }

    @Test
    void shouldCommitTransaction() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        tx.getConnection();
        tx.commit();

        verify(connection).commit();
        verify(connection).close();
    }

    @Test
    void shouldRollbackOnException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        tx.getConnection();
        tx.rollback();

        verify(connection).rollback();
        verify(connection).close();
    }

    @Test
    void shouldCloseTransaction() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        TransactionImpl tx = new TransactionImpl(dataSource);
        tx.getConnection();
        tx.close();

        assertTrue(tx.isClosed());
        verify(connection).close();
    }

    @Test
    void shouldLazyInitializeConnection() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);

        verify(dataSource, never()).getConnection();
        
        tx.getConnection();
        verify(dataSource).getConnection();
    }

    @Test
    void shouldSetAutoCommitFalse() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        tx.getConnection();

        verify(connection).setAutoCommit(false);
    }

    @Test
    void shouldNotCloseTwice() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        tx.getConnection();
        tx.close();
        tx.close();

        verify(connection, times(1)).close();
    }

    @Test
    void shouldRollbackOnCloseWithoutCommit() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);

        Transaction tx = new TransactionImpl(dataSource);
        tx.getConnection();
        tx.close();

        verify(connection).rollback();
        verify(connection).close();
    }

    @Test
    void shouldThrowOnNullDataSource() {
        assertThrows(NullPointerException.class, () -> new TransactionImpl(null));
    }
}