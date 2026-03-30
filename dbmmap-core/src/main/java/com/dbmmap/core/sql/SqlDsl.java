package com.dbmmap.core.sql;

import com.dbmmap.core.sql.dsl.*;

public class SqlDsl {
    public static SelectBuilder select(String... columns) {
        return new SelectBuilder(columns);
    }
    
    public static SelectBuilder selectAll() {
        return new SelectBuilder();
    }
    
    public static InsertBuilder insertInto(String table) {
        return new InsertBuilder(table);
    }
    
    public static UpdateBuilder update(String table) {
        return new UpdateBuilder(table);
    }
    
    public static DeleteBuilder deleteFrom(String table) {
        return new DeleteBuilder(table);
    }
}