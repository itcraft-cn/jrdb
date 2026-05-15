package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.ParamMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("StyleExecutor Test")
class StyleExecutorTest {

    @Test
    @DisplayName("JdbcStyleExecutor execute with null session should throw NullPointerException")
    void jdbcStyleExecutorNullSession() {
        JdbcStyleExecutor executor = new JdbcStyleExecutor();
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.setSql("SELECT 1");
        methodMeta.setOperationType(SqlOperationType.SELECT);

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
    @DisplayName("JdbcStyleExecutor execute with null sql should throw IllegalArgumentException")
    void jdbcStyleExecutorNullSql() {
        JdbcStyleExecutor executor = new JdbcStyleExecutor();
        Session session = mock(Session.class);
        MethodMeta methodMeta = new MethodMeta("test");

        assertThrows(IllegalArgumentException.class, () -> executor.execute(session, methodMeta, null));
    }

    @Test
    @DisplayName("MyBatisStyleExecutor convertToJdbcSql should replace #{param} with ?")
    void myBatisStyleExecutorConvertSql() {
        MyBatisStyleExecutor executor = new MyBatisStyleExecutor();
        
        String myBatisSql = "SELECT * FROM users WHERE id = #{id} AND name = #{name}";
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.setSql(myBatisSql);
        methodMeta.setOperationType(SqlOperationType.SELECT);
        methodMeta.addParamMeta(new ParamMeta(0, "id", Long.class));
        methodMeta.addParamMeta(new ParamMeta(1, "name", String.class));

        Session session = mock(Session.class);
        doReturn(1L).when(session).query(anyString(), any(Function.class));

        executor.execute(session, methodMeta, new Object[]{1L, "test"});

        verify(session).query(eq("SELECT * FROM users WHERE id = ? AND name = ?"), any(Function.class));
    }

    @Test
    @DisplayName("HibernateStyleExecutor convertHqlToSql should convert from Entity to SELECT * FROM table")
    void hibernateStyleExecutorConvertHql() {
        HibernateStyleExecutor executor = new HibernateStyleExecutor();

        String hql = "from User where id = ?";
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.setSql(hql);
        methodMeta.setOperationType(SqlOperationType.SELECT);

        Session session = mock(Session.class);
        doReturn(1L).when(session).query(anyString(), any(Function.class));

        executor.execute(session, methodMeta, new Object[]{1L});

        verify(session).query(eq("SELECT * FROM user WHERE id = ?"), any(Function.class));
    }

    @Test
    @DisplayName("HibernateStyleExecutor inferTableName should convert CamelCase to snake_case")
    void hibernateStyleExecutorInferTableName() {
        HibernateStyleExecutor executor = new HibernateStyleExecutor();

        String hql = "from UserAccount where id = ?";
        MethodMeta methodMeta = new MethodMeta("test");
        methodMeta.setSql(hql);
        methodMeta.setOperationType(SqlOperationType.SELECT);

        Session session = mock(Session.class);
        doReturn(1L).when(session).query(anyString(), any(Function.class));

        executor.execute(session, methodMeta, new Object[]{1L});

        verify(session).query(eq("SELECT * FROM user_account WHERE id = ?"), any(Function.class));
    }
}