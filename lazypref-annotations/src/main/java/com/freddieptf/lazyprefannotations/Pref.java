package com.freddieptf.lazyprefannotations;

/**
 * Created by fred on 6/5/17.
 */

public @interface Pref {
    String key() default "";

    boolean autoGenGet() default true;

    Class<? extends TypeConverter> converter() default TypeConverter.class;
}
