package com.freddieptf.sample.converter;

import com.freddieptf.lazyprefannotations.TypeConverter;
import com.freddieptf.sample.model.User;

/**
 * Created by fred on 6/10/17.
 */

public class UserPrefConverter implements TypeConverter<User, String> {

    public UserPrefConverter() {
    }

    @Override
    public String toSupportedType(User val) {
        String s = val.name + "-" + val.id;
        return s;
    }

    @Override
    public User getVal(String val) {
        String[] strings = val.split("-");
        return new User(strings[0], strings[1]);
    }
}
