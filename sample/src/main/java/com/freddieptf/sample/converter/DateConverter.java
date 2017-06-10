package com.freddieptf.sample.converter;

import com.freddieptf.lazyprefannotations.TypeConverter;

import java.util.Date;

/**
 * Created by fred on 6/10/17.
 */

public class DateConverter implements TypeConverter<Date, Long> {
    @Override
    public Long toSupportedType(Date val) {
        return val.getTime();
    }

    @Override
    public Date getVal(Long val) {
        return new Date(val);
    }
}
