package cn.itcraft.dbmmap.core.sql.dsl;

import cn.itcraft.dbmmap.core.sql.BoundSql;
import java.util.*;

public class UpdateBuilder {
    private final String table;
    private final List<String> setClauses = new ArrayList<>();
    private final List<Object> setParams = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    
    public UpdateBuilder(String table) {
        this.table = table;
    }
    
    public UpdateBuilder set(String column, Object value) {
        setClauses.add(column + " = ?");
        setParams.add(value);
        return this;
    }
    
    public UpdateBuilder setRaw(String clause) {
        setClauses.add(clause);
        return this;
    }
    
    public UpdateBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));
        return this;
    }
    
    public UpdateBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }
    
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(table).append(" SET ");
        sql.append(String.join(", ", setClauses));
        
        List<Object> allParams = new ArrayList<>(setParams);
        
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            allParams.addAll(whereParams);
        }
        
        return new BoundSql(sql.toString(), allParams.toArray());
    }
}