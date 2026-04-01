package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.benchmark.entity.User;
import cn.itcraft.jrdb.benchmark.entity.UserEntity;
import cn.itcraft.jrdb.jdbc.config.CacheLevel;
import cn.itcraft.jrdb.jdbc.config.JrdbConfig;
import cn.itcraft.jrdb.jdbc.mapping.EntityMapper;
import cn.itcraft.jrdb.jdbc.mapping.MapperRegistry;
import cn.itcraft.jrdb.jdbc.memory.MemoryPool;
import cn.itcraft.jrdb.jdbc.memory.MemoryPoolConfig;
import cn.itcraft.jrdb.jdbc.query.QueryResult;
import cn.itcraft.jrdb.jdbc.session.SessionImpl;
import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class EntityMappingCorrectnessTest {

    public static void main(String[] args) throws SQLException {
        DataSource dataSource = createDataSource();
        TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
        MapperRegistry mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        
        System.out.println("=== 正确性验证 ===\n");
        
        testJrdbRaw(dataSource, mapperRegistry);
        testJrdbAsm(dataSource, mapperRegistry);
        testJdbcManual(dataSource);
        testMyBatis(dataSource);
        testHibernate();
        
        System.out.println("\n=== 所有测试通过 ===");
    }

    static DataSource createDataSource() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:correctness;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name VARCHAR(100), " +
                "age INT, " +
                "email VARCHAR(200), " +
                "status VARCHAR(20))");
            stmt.execute("INSERT INTO users (name, age, email, status) VALUES ('Alice', 25, 'alice@test.com', 'active')");
            stmt.execute("INSERT INTO users (name, age, email, status) VALUES ('Bob', 30, 'bob@test.com', 'inactive')");
        }
        return ds;
    }

    static void testJrdbRaw(DataSource dataSource, MapperRegistry mapperRegistry) throws SQLException {
        System.out.println("1. jrdb_raw 测试:");
        
        MemoryPoolConfig poolConfig = MemoryPoolConfig.builder()
            .preAllocMemSize(64 * 1024 * 1024)
            .queryCacheSize(4 * 1024 * 1024)
            .build();
        MemoryPool memoryPool = new MemoryPool(poolConfig);
        
        JrdbConfig config = JrdbConfig.builder()
            .cacheLevel(CacheLevel.L1)
            .build();
        
        try (SessionImpl session = new SessionImpl(memoryPool, config, dataSource, mapperRegistry)) {
            try (QueryResult result = session.query("SELECT id, name, age, email, status FROM users WHERE id = ?", 1L)) {
                if (result != null && result.next()) {
                    long id = result.getLong("ID");
                    String name = result.getString("NAME");
                    int age = result.getInt("AGE");
                    String email = result.getString("EMAIL");
                    String status = result.getString("STATUS");
                    
                    System.out.println("   id=" + id + ", name=" + name + ", age=" + age + ", email=" + email + ", status=" + status);
                    assert id == 1L : "id应该是1";
                    assert "Alice".equals(name) : "name应该是Alice";
                    assert age == 25 : "age应该是25";
                    assert "alice@test.com".equals(email) : "email不匹配";
                    assert "active".equals(status) : "status不匹配";
                    System.out.println("   ✓ 单条查询正确");
                }
            }

            try (QueryResult result = session.query("SELECT id, name, age, email, status FROM users ORDER BY id")) {
                int count = 0;
                while (result != null && result.next()) {
                    count++;
                    long id = result.getLong("ID");
                    String name = result.getString("NAME");
                    System.out.println("   row" + count + ": id=" + id + ", name=" + name);
                }
                assert count == 2 : "应该有2条记录";
                System.out.println("   ✓ 列表查询正确，共" + count + "条");
            }
        }
        memoryPool.destroy();
        System.out.println();
    }

    static void testJrdbAsm(DataSource dataSource, MapperRegistry mapperRegistry) throws SQLException {
        System.out.println("2. jrdb_asm 测试:");
        
        EntityMapper<User> mapper = mapperRegistry.getMapper(User.class);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            ps.setLong(1, 1L);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapper.mapFromResultSet(rs);
                    System.out.println("   " + user);
                    assert user.getId() == 1L : "id应该是1";
                    assert "Alice".equals(user.getName()) : "name应该是Alice";
                    assert user.getAge() == 25 : "age应该是25";
                    assert "alice@test.com".equals(user.getEmail()) : "email不匹配";
                    assert "active".equals(user.getStatus()) : "status不匹配";
                    System.out.println("   ✓ ASM映射正确");
                }
            }
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                User user = mapper.mapFromResultSet(rs);
                count++;
                System.out.println("   row" + count + ": " + user);
            }
            assert count == 2 : "应该有2条记录";
            System.out.println("   ✓ ASM列表映射正确，共" + count + "条");
        }
        System.out.println();
    }

    static void testJdbcManual(DataSource dataSource) throws SQLException {
        System.out.println("3. jdbc_manual 测试:");
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            ps.setLong(1, 1L);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setName(rs.getString("name"));
                    user.setAge(rs.getInt("age"));
                    user.setEmail(rs.getString("email"));
                    user.setStatus(rs.getString("status"));
                    
                    System.out.println("   " + user);
                    assert user.getId() == 1L : "id应该是1";
                    assert "Alice".equals(user.getName()) : "name应该是Alice";
                    assert user.getAge() == 25 : "age应该是25";
                    System.out.println("   ✓ JDBC手动映射正确");
                }
            }
        }
        System.out.println();
    }

    static void testMyBatis(DataSource dataSource) {
        System.out.println("4. myBatis 测试:");
        
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        config.setCacheEnabled(false);
        config.setLocalCacheScope(org.apache.ibatis.session.LocalCacheScope.STATEMENT);
        config.addMapper(UserMapper.class);
        
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
        try (SqlSession session = factory.openSession()) {
            User user = session.selectOne("cn.itcraft.jrdb.benchmark.EntityMappingCorrectnessTest$UserMapper.getById", 1L);
            System.out.println("   " + user);
            assert user.getId() == 1L : "id应该是1";
            assert "Alice".equals(user.getName()) : "name应该是Alice";
            assert user.getAge() == 25 : "age应该是25";
            System.out.println("   ✓ MyBatis映射正确");
            
            List<User> users = session.selectList("cn.itcraft.jrdb.benchmark.EntityMappingCorrectnessTest$UserMapper.getAll");
            System.out.println("   列表查询共" + users.size() + "条");
            assert users.size() == 2 : "应该有2条记录";
            System.out.println("   ✓ MyBatis列表映射正确");
        }
        System.out.println();
    }

    static void testHibernate() {
        System.out.println("5. hibernate 测试:");
        
        org.hibernate.cfg.Configuration config = new org.hibernate.cfg.Configuration();
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:correctness;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.connection.username", "sa");
        config.setProperty("hibernate.connection.password", "");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        config.setProperty("hibernate.cache.use_second_level_cache", "false");
        config.setProperty("hibernate.cache.use_query_cache", "false");
        config.setProperty("hibernate.show_sql", "false");
        config.addAnnotatedClass(UserEntity.class);
        
        try (org.hibernate.SessionFactory factory = config.buildSessionFactory();
             org.hibernate.Session session = factory.openSession()) {
            
            UserEntity user = session.get(UserEntity.class, 1L);
            System.out.println("   " + user);
            assert user.getId() == 1L : "id应该是1";
            assert "Alice".equals(user.getName()) : "name应该是Alice";
            assert user.getAge() == 25 : "age应该是25";
            System.out.println("   ✓ Hibernate映射正确");
            
            List<UserEntity> users = session.createQuery("FROM UserEntity", UserEntity.class).list();
            System.out.println("   列表查询共" + users.size() + "条");
            assert users.size() == 2 : "应该有2条记录";
            System.out.println("   ✓ Hibernate列表映射正确");
        }
        System.out.println();
    }

    public interface UserMapper {
        @Select("SELECT id, name, age, email, status FROM users WHERE id = #{id}")
        User getById(Long id);

        @Select("SELECT id, name, age, email, status FROM users ORDER BY id")
        List<User> getAll();
    }
}