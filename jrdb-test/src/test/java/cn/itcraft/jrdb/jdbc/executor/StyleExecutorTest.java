package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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

    @Test
    @DisplayName("bindParametersFromCache should use cached param mapping")
    void bindParametersFromCache() {
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.addParamMapping(0);
        methodMeta.addParamMapping(1);
        
        MyBatisStyleExecutor executor = new MyBatisStyleExecutor();
        Object[] params = executor.bindParametersFromCache(methodMeta, new Object[]{"id1", "name2"});
        
        assertEquals(2, params.length);
        assertEquals("id1", params[0]);
        assertEquals("name2", params[1]);
    }

    @Test
    @DisplayName("bindParametersFromCache with insufficient args should throw IllegalArgumentException")
    void bindParametersFromCacheInsufficientArgs() {
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.addParamMapping(0);
        methodMeta.addParamMapping(1);
        
        MyBatisStyleExecutor executor = new MyBatisStyleExecutor();
        
        assertThrows(IllegalArgumentException.class, () -> executor.bindParametersFromCache(methodMeta, new Object[]{"id1"}));
    }
}