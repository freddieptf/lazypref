package com.freddieptf.sample;

import com.freddieptf.lazyprefannotations.LazyPref;
import com.freddieptf.lazyprefannotations.Pref;
import com.freddieptf.sample.converter.DateConverter;
import com.freddieptf.sample.converter.IntArrayConverter;
import com.freddieptf.sample.converter.UserPrefConverter;
import com.freddieptf.sample.model.User;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * Created by fred on 6/2/17.
 */
@LazyPref
public interface SharedPrefs {

    @Pref(key = "nums")
    int number = 0;

    @Pref
    float numFloat = 0;

    @Pref
    boolean isPerson = false;

    @Pref
    long time = 0;

    @Pref
    String name = "";

    @Pref
    Set<String> stringSet = Collections.EMPTY_SET;

    @Pref(converter = IntArrayConverter.class)
    int[] ageArray = new int[]{};

    @Pref(converter = UserPrefConverter.class)
    User primaryUser = null;

    @Pref(converter = DateConverter.class)
    Date lastLogin = null;
}
