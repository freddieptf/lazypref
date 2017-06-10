package com.freddieptf.lazyprefannotations;

/**
 * Created by fred on 6/8/17.
 */

public interface TypeConverter<T, E> {
    E toSupportedType(T val);

    T getVal(E val);
}
