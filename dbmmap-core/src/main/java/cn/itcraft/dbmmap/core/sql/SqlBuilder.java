package cn.itcraft.dbmmap.core.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SqlBuilder {
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    
    public static SqlBuilder create() { 
        return new SqlBuilder(); 
    }
    
    public SqlBuilder append(String fragment, Object... params) {
        if (sql.length() > 0 && !fragment.startsWith(" ")) {
            sql.append(" ");
        }
        sql.append(fragment);
        this.params.addAll(Arrays.asList(params));
        return this;
    }
    
    public SqlBuilder appendIf(boolean condition, String fragment, Object... params) {
        if (condition) {
            append(fragment, params);
        }
        return this;
    }
    
    public String build() { 
        return sql.toString(); 
    }
    
    public Object[] getParams() { 
        return params.toArray(); 
    }
    
    public void reset() { 
        sql.setLength(0); 
        params.clear(); 
    }
    
    public boolean isEmpty() { 
        return sql.length() == 0; 
    }
}