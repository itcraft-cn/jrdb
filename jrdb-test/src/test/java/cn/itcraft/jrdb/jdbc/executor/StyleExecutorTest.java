package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("StyleExecutor Test")
class StyleExecutorTest {

    @Test
    @DisplayName("JdbcStyleExecutor execute with null session should throw NullPointerException")
    void jdbcStyleExecutorNullSession() {
        JdbcStyleExecutor executor = new JdbcStyleExecutor();
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.setCachedSql("SELECT 1");
        methodMeta.setOperationType(SqlOperationType.SELECT);
        methodMeta.setReturnType(Long.class);

        assertThrows(NullPointerException.class, () -> executor.execute(null, methodMeta, null));
    }

    @Test
    @DisplayName("JdbcStyleExecutor execute with null methodMeta should throw NullPointerException")
    void jdbcStyleExecutorNullMethodMeta() {
        JdbcStyleExecutor executor = new JdbcStyleExecutor();
        Session session = mock(Session.class);

        assertThrows(NullPointerException.class, () -> executor.execute(session, null, null));
    }

    @Test
    @DisplayName("JdbcStyleExecutor execute with null cachedSql should throw IllegalArgumentException")
    void jdbcStyleExecutorNullCachedSql() {
        JdbcStyleExecutor executor = new JdbcStyleExecutor();
        Session session = mock(Session.class);
        MethodMeta methodMeta = new MethodMeta("test");

        assertThrows(IllegalArgumentException.class, () -> executor.execute(session, methodMeta, null));
    }
}