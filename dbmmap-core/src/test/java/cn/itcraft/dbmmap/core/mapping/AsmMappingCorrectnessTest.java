package cn.itcraft.dbmmap.core.mapping;

import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class AsmMappingCorrectnessTest {

    @Test
    void testAsmMappingCorrectness() throws Exception {
        String url = "jdbc:h2:mem:asmtest;DB_CLOSE_DELAY=-1";
        
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE users (id BIGINT, name VARCHAR(100), age INT)");
            stmt.execute("INSERT INTO users VALUES (1, 'Alice', 25)");
            stmt.execute("INSERT INTO users VALUES (2, 'Bob', 30)");
        }

        TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
        MapperRegistry mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        EntityMapper<User> mapper = mapperRegistry.getMapper(User.class);

        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, age FROM users WHERE id = 1")) {
            
            assertTrue(rs.next(), "应该有数据");
            
            User user = mapper.mapFromResultSet(rs);
            
            System.out.println("ASM映射结果: id=" + user.id + ", name=" + user.name + ", age=" + user.age);
            
            assertEquals(1L, user.id, "id应该是1");
            assertEquals("Alice", user.name, "name应该是Alice");
            assertEquals(25, user.age, "age应该是25");
        }

        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, age FROM users ORDER BY id")) {
            
            assertTrue(rs.next());
            User user1 = mapper.mapFromResultSet(rs);
            assertEquals(1L, user1.id);
            assertEquals("Alice", user1.name);
            
            assertTrue(rs.next());
            User user2 = mapper.mapFromResultSet(rs);
            assertEquals(2L, user2.id);
            assertEquals("Bob", user2.name);
            
            System.out.println("ASM列表映射正确: 共2条记录");
        }
    }

    public static class User {
        private Long id;
        private String name;
        private Integer age;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}