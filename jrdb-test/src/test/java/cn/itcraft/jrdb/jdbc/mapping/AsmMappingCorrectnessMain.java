package cn.itcraft.jrdb.jdbc.mapping;

import cn.itcraft.jrdb.jdbc.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AsmMappingCorrectnessMain {

    public static void main(String[] args) throws Exception {
        System.out.println("=== jrdb_asm 正确性验证 ===\n");

        String url = "jdbc:h2:mem:asmmain;DB_CLOSE_DELAY=-1";
        
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE users (id BIGINT, name VARCHAR(100), age INT, email VARCHAR(200), status VARCHAR(20))");
            stmt.execute("INSERT INTO users VALUES (1, 'Alice', 25, 'alice@test.com', 'active')");
            stmt.execute("INSERT INTO users VALUES (2, 'Bob', 30, 'bob@test.com', 'inactive')");
        }

        TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
        MapperRegistry mapperRegistry = new MapperRegistry(typeHandlerRegistry);
        EntityMapper<TestUser> mapper = mapperRegistry.getMapper(TestUser.class);

        System.out.println("1. 单条查询测试:");
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, age, email, status FROM users WHERE id = 1")) {
            
            if (rs.next()) {
                TestUser user = mapper.mapFromResultSet(rs);
                System.out.println("   ASM映射结果: id=" + user.id + ", name=" + user.name + ", age=" + user.age + ", email=" + user.email + ", status=" + user.status);
                
                assert user.id == 1L : "id应该是1";
                assert "Alice".equals(user.name) : "name应该是Alice";
                assert user.age == 25 : "age应该是25";
                assert "alice@test.com".equals(user.email) : "email不匹配";
                assert "active".equals(user.status) : "status不匹配";
                System.out.println("   ✓ ASM单条查询正确\n");
            }
        }

        System.out.println("2. 列表查询测试:");
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, age, email, status FROM users ORDER BY id")) {
            
            int count = 0;
            while (rs.next()) {
                TestUser user = mapper.mapFromResultSet(rs);
                count++;
                System.out.println("   row" + count + ": id=" + user.id + ", name=" + user.name + ", age=" + user.age);
            }
            assert count == 2 : "应该有2条记录";
            System.out.println("   ✓ ASM列表查询正确，共" + count + "条\n");
        }

        System.out.println("=== ASM映射正确性验证通过 ===");
    }

    public static class TestUser {
        private Long id;
        private String name;
        private Integer age;
        private String email;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}