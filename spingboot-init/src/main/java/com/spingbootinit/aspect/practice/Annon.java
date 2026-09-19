package com.spingbootinit.aspect.practice;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Annon {

    String value() default "";

}
