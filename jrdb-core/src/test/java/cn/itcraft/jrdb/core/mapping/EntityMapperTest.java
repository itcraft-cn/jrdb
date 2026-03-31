package cn.itcraft.jrdb.core.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EntityMapper interface contract.
 *
 * @author jrdb
 * @since 1.0.0
 */
class EntityMapperTest {

    @Test
    @DisplayName("EntityMapper interface defines mapFromResultSet")
    void testInterfaceHasMapFromResultSet() throws NoSuchMethodException {
        assertEquals(ResultSet.class, 
            EntityMapper.class.getMethod("mapFromResultSet", ResultSet.class)
                .getParameterTypes()[0]);
    }

    @Test
    @DisplayName("EntityMapper interface defines mapToRow")
    void testInterfaceHasMapToRow() throws NoSuchMethodException {
        assertEquals(Object[].class,
            EntityMapper.class.getMethod("mapToRow", Object.class)
                .getReturnType());
    }

    @Test
    @DisplayName("EntityMapper is a generic interface")
    void testInterfaceIsGeneric() {
        assertTrue(EntityMapper.class.getTypeParameters().length > 0);
        assertEquals("T", EntityMapper.class.getTypeParameters()[0].getName());
    }

    @Test
    @DisplayName("mapFromResultSet throws SQLException")
    void testMapFromResultSetThrowsSQLException() throws NoSuchMethodException {
        assertTrue(EntityMapper.class.getMethod("mapFromResultSet", ResultSet.class)
            .getExceptionTypes()[0] == SQLException.class);
    }

    @Test
    @DisplayName("Simple implementation can map entity")
    void testSimpleImplementation() throws SQLException {
        EntityMapper<TestEntity> mapper = new TestEntityMapper();
        
        TestEntity entity = mapper.mapFromResultSet(null);
        assertNotNull(entity);
        
        Object[] values = mapper.mapToRow(entity);
        assertNotNull(values);
        assertEquals(2, values.length);
    }

    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static class TestEntityMapper implements EntityMapper<TestEntity> {
        @Override
        public TestEntity mapFromResultSet(ResultSet rs) throws SQLException {
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setName("test");
            return entity;
        }

        @Override
        public Object[] mapToRow(TestEntity entity) {
            return new Object[]{entity.getId(), entity.getName()};
        }
    }
}