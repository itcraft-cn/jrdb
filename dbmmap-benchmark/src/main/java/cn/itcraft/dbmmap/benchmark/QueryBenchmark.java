package cn.itcraft.dbmmap.benchmark;

import cn.itcraft.dbmmap.benchmark.entity.User;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class QueryBenchmark extends BenchmarkBase {

    private Connection connection;

    @Setup(Level.Trial)
    public void setup() throws SQLException {
        initDatabase();
        connection = getConnection();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        closeConnection(connection);
    }

    @Benchmark
    public void jdbc_SingleQuery(Blackhole bh) throws SQLException {
        String sql = "SELECT id, name, age, email, status FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bh.consume(rs.getLong("id"));
                    bh.consume(rs.getString("name"));
                    bh.consume(rs.getInt("age"));
                    bh.consume(rs.getString("email"));
                    bh.consume(rs.getString("status"));
                }
            }
        }
    }

    @Benchmark
    public void jdbc_QueryAll(Blackhole bh) throws SQLException {
        String sql = "SELECT id, name, age, email, status FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bh.consume(rs.getLong("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("age"));
            }
        }
    }

    @Benchmark
    public List<User> jdbc_QueryToList() throws SQLException {
        String sql = "SELECT id, name, age, email, status FROM users LIMIT 100";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setAge(rs.getInt("age"));
                user.setEmail(rs.getString("email"));
                user.setStatus(rs.getString("status"));
                users.add(user);
            }
        }
        return users;
    }
}