package cn.itcraft.jrdb.core.sql.dsl;

import cn.itcraft.jrdb.core.sql.BoundSql;
import java.util.*;

public class DeleteBuilder {
    private final String table;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    
    public DeleteBuilder(String table) {
        this.table = table;
    }
    
    public DeleteBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));
        return this;
    }
    
    public DeleteBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }
    
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(table);
        
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
        
        return new BoundSql(sql.toString(), whereParams.toArray());
    }
}