package com.dbmmap.core.sql;

public class BoundSql {
    private final String sql;
    private final Object[] params;
    
    public BoundSql(String sql, Object[] params) {
        this.sql = sql;
        this.params = params != null ? params : new Object[0];
    }
    
    public String getSql() { 
        return sql; 
    }
    
    public Object[] getParams() { 
        return params; 
    }
}