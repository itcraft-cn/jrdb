package com.dbmmap.springboot.repository;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Indexed
public @interface DbMmapRepository {
    String value() default "";
}