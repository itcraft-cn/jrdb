package cn.itcraft.jrdb.jdbc.dao;

import cn.itcraft.jrdb.jdbc.annotation.SqlParamBindStyle;
import cn.itcraft.jrdb.jdbc.executor.StyleExecutor;
import cn.itcraft.jrdb.jdbc.integration.PublicUserEntity;
import cn.itcraft.jrdb.jdbc.session.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DaoProxyTemplate Test")
class DaoProxyTemplateTest {

    @Test
    @DisplayName("constructor should build methodNameMap")
    void constructorBuildsMethodNameMap() {
        DaoMeta daoMeta = new DaoMeta(TestDao.class);
        Session session = mock(Session.class);
        StyleExecutor executor = mock(StyleExecutor.class);
        
        DaoProxyTemplate template = new DaoProxyTemplate(daoMeta, session, executor);
        
        Map<Integer, Method> methodNameMap = template.getMethodNameMap();
        assertNotNull(methodNameMap);
        assertTrue(methodNameMap.containsKey("findById".hashCode()));
    }

    interface TestDao {
        PublicUserEntity findById(Long id);
    }
}