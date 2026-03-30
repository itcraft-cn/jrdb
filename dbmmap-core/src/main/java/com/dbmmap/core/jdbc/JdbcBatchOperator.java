package com.dbmmap.core.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Helly Guo
 * <p>
 * Created on 3/3/22 6:44 PM
 */
public class JdbcBatchOperator implements BatchOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcBatchOperator.class);

    private static final int COMMIT_SIZE = 1000;

    private static final Map<String, JdbcSqlBox<?>> SQL_BOX_MAP = new HashMap<>();

    private DataSource dataSource;

    public static <T> void register(String id, JdbcSqlBox<T> sqlBox) {
        SQL_BOX_MAP.put(id, sqlBox);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private <T> List<Integer> execModify(String id, List<T> list) {
        Connection conn = JdbcHelper.openLink(dataSource);
        JdbcSqlBox<T> box = (JdbcSqlBox<T>) SQL_BOX_MAP.get(id);
        List<Integer> effects = JdbcHelper.batchModify(conn, box.sqlTemplate(), conv(list, box), COMMIT_SIZE);
        JdbcHelper.closeLink(conn);
        return effects;
    }

    @Override
    public <T> void batchInsert(String id, List<T> list) {
        execModify(id, list);
    }

    @Override
    public <T> int batchUpdate(String id, List<T> list) {
        return execModify(id, list).stream()
                                   .mapToInt(v -> v)
                                   .sum();
    }

    private <T> List<Object[]> conv(List<T> list, JdbcSqlBox<T> box) {
        int paramSize = box.paramSize();
        return list.stream()
                   .map(bean -> box.fetchParam(bean, paramSize))
                   .collect(Collectors.toList());
    }
}