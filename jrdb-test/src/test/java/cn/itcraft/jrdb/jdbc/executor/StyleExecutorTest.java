package cn.itcraft.jrdb.jdbc.executor;

import cn.itcraft.jrdb.jdbc.dao.MethodMeta;
import cn.itcraft.jrdb.jdbc.dao.ParamMeta;
import cn.itcraft.jrdb.jdbc.dao.SqlOperationType;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        String jdbcSql = executor.convertToJdbcSql(myBatisSql);
        
        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", jdbcSql);
    }

    @Test
    @DisplayName("HibernateStyleExecutor convertHqlToSql should convert from Entity to SELECT * FROM table")
    void hibernateStyleExecutorConvertHql() {
        HibernateStyleExecutor executor = new HibernateStyleExecutor();

        String hql = "from User where id = ?";
        String sql = executor.convertHqlToSql(hql);
        
        assertEquals("SELECT * FROM user WHERE id = ?", sql);
    }

    @Test
    @DisplayName("HibernateStyleExecutor inferTableName should convert CamelCase to snake_case")
    void hibernateStyleExecutorInferTableName() {
        HibernateStyleExecutor executor = new HibernateStyleExecutor();

        String hql = "from UserAccount where id = ?";
        String sql = executor.convertHqlToSql(hql);
        
        assertEquals("SELECT * FROM user_account WHERE id = ?", sql);
    }
}