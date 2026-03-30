package com.dbmmap.springboot.entity;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DbMmapEntity {
    String tableName() default "";
}