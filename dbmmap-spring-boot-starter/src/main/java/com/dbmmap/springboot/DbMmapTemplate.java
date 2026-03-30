package com.dbmmap.springboot;

import com.dbmmap.core.mapping.EntityMapper;
import com.dbmmap.core.mapping.MapperRegistry;
import com.dbmmap.core.query.QueryResult;
import com.dbmmap.core.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class DbMmapTemplate {

    private final Session session;
    private final MapperRegistry mapperRegistry;
    private final DataSource dataSource;

    public DbMmapTemplate(Session session, MapperRegistry mapperRegistry, DataSource dataSource) {
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