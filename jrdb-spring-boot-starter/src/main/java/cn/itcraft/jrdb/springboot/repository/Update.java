package cn.itcraft.jrdb.springboot.repository;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {
    String value();
}