package cn.itcraft.jrdb.springboot.entity;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JrdbEntity {
    String tableName() default "";
}