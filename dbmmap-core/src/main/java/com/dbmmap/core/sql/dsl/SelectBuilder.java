package com.dbmmap.core.sql.dsl;

import com.dbmmap.core.sql.BoundSql;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectBuilder {
    private String[] columns;
    private String table;
    private String alias;
    private final List<String> joins = new ArrayList<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private String groupBy;
    private String having;
    private final List<Object> havingParams = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Integer offset;
    
    public SelectBuilder(String... columns) {
        this.columns = columns;
    }
    
    public SelectBuilder from(String table) {
        this.table = table;
        return this;
    }
    
    public SelectBuilder from(String table, String alias) {
        this.table = table;
        this.alias = alias;
        return this;
    }
    
    public SelectBuilder join(String table, String on) {
        joins.add("JOIN " + table + " ON " + on);
        return this;
    }
    
    public SelectBuilder leftJoin(String table, String on) {
        joins.add("LEFT JOIN " + table + " ON " + on);
        return this;
    }
    
    public SelectBuilder rightJoin(String table, String on) {
        joins.add("RIGHT JOIN " + table + " ON " + on);
        return this;
    }
    
    public SelectBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));
        return this;
    }
    
    public SelectBuilder whereIf(boolean condition, String clause, Object... params) {
        if (condition) {
            where(clause, params);
        }
        return this;
    }
    
    public SelectBuilder groupBy(String... columns) {
        this.groupBy = String.join(", ", columns);
        return this;
    }
    
    public SelectBuilder having(String condition, Object... params) {
        this.having = condition;
        havingParams.addAll(Arrays.asList(params));
        return this;
    }
    
    public SelectBuilder orderBy(String... orders) {
        this.orderBy = String.join(", ", orders);
        return this;
    }
    
    public SelectBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    public SelectBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    public BoundSql build() {
        StringBuilder sql = new StringBuilder("SELECT ");
        List<Object> allParams = new ArrayList<>();
        
        sql.append(columns == null || columns.length == 0 ? "*" : String.join(", ", columns));
        
        sql.append(" FROM ").append(table);
        if (alias != null) sql.append(" ").append(alias);
        
        for (String join : joins) {
            sql.append(" ").append(join);
        }
        
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            allParams.addAll(whereParams);
        }
        
        if (groupBy != null) {
            sql.append(" GROUP BY ").append(groupBy);
        }
        
        if (having != null) {
            sql.append(" HAVING ").append(having);
            allParams.addAll(havingParams);
        }
        
        if (orderBy != null) {
            sql.append(" ORDER BY ").append(orderBy);
        }
        
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return new BoundSql(sql.toString(), allParams.toArray());
    }
}