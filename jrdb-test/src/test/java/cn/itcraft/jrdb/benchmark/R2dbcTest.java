package cn.itcraft.jrdb.benchmark;

import cn.itcraft.jrdb.r2dbc.session.R2dbcSession;
import cn.itcraft.jrdb.r2dbc.session.R2dbcSessionImpl;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class R2dbcTest {

    private R2dbcSession session;
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() throws Exception {
        connectionFactory = H2ConnectionFactory.inMemory("test_" + System.nanoTime());
        session = new R2dbcSessionImpl(connectionFactory);

        session.update("CREATE TABLE users (id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), age INT, email VARCHAR(200))").get();
        session.update("INSERT INTO users (name, age, email) VALUES ('Alice', 25, 'alice@test.com')").get();
        session.update("INSERT INTO users (name, age, email) VALUES ('Bob', 30, 'bob@test.com')").get();
        session.update("INSERT INTO users (name, age, email) VALUES ('Charlie', 35, 'charlie@test.com')").get();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void testQueryOne() throws Exception {
        String name = session.queryOne("SELECT name FROM users WHERE id = 1", String.class).get();
        assertEquals("Alice", name);
    }

    @Test
    void testQueryList() throws Exception {
        List<String> names = session.queryList("SELECT name FROM users ORDER BY id", String.class).get();
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), names);
    }

    @Test
    void testQueryWithParams() throws Exception {
        Integer age = session.queryOne("SELECT age FROM users WHERE name = $1", Integer.class, "Alice").get();
        assertEquals(25, age);
    }

    @Test
    void testUpdate() throws Exception {
        Integer updated = session.update("UPDATE users SET age = $1 WHERE name = $2", 26, "Alice").get();
        assertEquals(1, updated);

        Integer newAge = session.queryOne("SELECT age FROM users WHERE name = $1", Integer.class, "Alice").get();
        assertEquals(26, newAge);
    }

    @Test
    void testInsert() throws Exception {
        Boolean inserted = session.insert("INSERT INTO users (name, age, email) VALUES ($1, $2, $3)", "David", 40, "david@test.com").get();
        assertTrue(inserted);

        Number count = session.queryOne("SELECT COUNT(*) FROM users", Number.class).get();
        assertEquals(4, count.intValue());
    }

    @Test
    void testInsertWithKey() throws Exception {
        Number id = session.insertWithKey("INSERT INTO users (name, age, email) VALUES ($1, $2, $3)", Number.class, "Eve", 28, "eve@test.com").get();
        assertNotNull(id);
        assertTrue(id.longValue() > 0);
    }

    @Test
    void testDelete() throws Exception {
        Integer deleted = session.delete("DELETE FROM users WHERE name = $1", "Bob").get();
        assertEquals(1, deleted);

        Number count = session.queryOne("SELECT COUNT(*) FROM users", Number.class).get();
        assertEquals(2, count.intValue());
    }

    @Test
    void testBatch() throws Exception {
        List<Object[]> paramsList = Arrays.asList(
            new Object[]{"Updated1", 1L},
            new Object[]{"Updated2", 2L}
        );

        int[] results = session.batch("UPDATE users SET name = $1 WHERE id = $2", paramsList).get();
        assertEquals(2, results.length);
        assertEquals(1, results[0]);
        assertEquals(1, results[1]);
    }
}