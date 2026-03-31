package cn.itcraft.jrdb.springboot;

import cn.itcraft.jrdb.core.mapping.EntityMapper;
import cn.itcraft.jrdb.core.mapping.MapperRegistry;
import cn.itcraft.jrdb.core.query.QueryResult;
import cn.itcraft.jrdb.core.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class JrdbTemplate {

    private final Session session;
    private final MapperRegistry mapperRegistry;
    private final DataSource dataSource;

    public JrdbTemplate(Session session, MapperRegistry mapperRegistry, DataSource dataSource) {
        this.session = session;
        this.mapperRegistry = mapperRegistry;
        this.dataSource = dataSource;
    }

    public <T> T queryForObject(String sql, Class<T> entityType, Object... params) {
        EntityMapper<T> mapper = mapperRegistry.getMapper(entityType);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper.mapFromResultSet(rs);
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    public <T> List<T> queryForList(String sql, Class<T> entityType, Object... params) {
        EntityMapper<T> mapper = mapperRegistry.getMapper(entityType);
        List<T> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapper.mapFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return list;
    }

    public int update(String sql, Object... params) {
        return session.update(sql, params);
    }

    public Session getSession() {
        return session;
    }

    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }
}