package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.benchmark.entity.User;
import cn.itcraft.jrdb.core.mapping.EntityMapper;
import cn.itcraft.jrdb.core.mapping.MapperRegistry;
import cn.itcraft.jrdb.core.type.TypeHandlerRegistry;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class QuickPerfTest {

    public static void main(String[] args) throws Exception {
        DataSource ds = createDataSource();
        
        TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
        MapperRegistry mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        EntityMapper<User> mapper = mapperRegistry.getMapper(User.class);

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            
            long start = System.nanoTime();
            int iterations = 100000;
            
            for (int i = 0; i < iterations; i++) {
                ps.setLong(1, 1L);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User user = mapper.mapFromResultSet(rs);
                    }
                }
            }
            
            long elapsed = System.nanoTime() - start;
            double opsPerSec = iterations * 1_000_000_000.0 / elapsed;
            
            System.out.printf("ASM映射: %.0f ops/s (每次 %.2f us)%n", opsPerSec, elapsed / (iterations * 1000.0));
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, age, email, status FROM users WHERE id = ?")) {
            
            long start = System.nanoTime();
            int iterations = 100000;
            
            for (int i = 0; i < iterations; i++) {
                ps.setLong(1, 1L);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User user = new User();
                        user.setId(rs.getLong("id"));
                        user.setName(rs.getString("name"));
                        user.setAge(rs.getInt("age"));
                        user.setEmail(rs.getString("email"));
                        user.setStatus(rs.getString("status"));
                    }
                }
            }
            
            long elapsed = System.nanoTime() - start;
            double opsPerSec = iterations * 1_000_000_000.0 / elapsed;
            
            System.out.printf("JDBC手动映射: %.0f ops/s (每次 %.2f us)%n", opsPerSec, elapsed / (iterations * 1000.0));
        }
    }

    static DataSource createDataSource() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:quickperf;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), age INT, email VARCHAR(200), status VARCHAR(20))");
            stmt.execute("INSERT INTO users VALUES (1, 'Alice', 25, 'alice@test.com', 'active')");
        }
        return ds;
    }
}