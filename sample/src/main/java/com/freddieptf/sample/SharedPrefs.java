package com.freddieptf.sample;

import com.freddieptf.lazyprefannotations.GetPref;
import com.freddieptf.lazyprefannotations.LazyPref;
import com.freddieptf.lazyprefannotations.SavePref;

import java.util.Set;

/**
 * Created by fred on 6/2/17.
 */
@LazyPref
public interface SharedPrefs {

    @SavePref(name = "number", getter = "getNumber")
    void saveNumber(int number);

    @SavePref(name = "sets", getter = "getMySet")
    void saveMySet(Set<String> stringSet);

    @SavePref(name = "string", getter = "getMyString")
    void saveMyString(String string);

    @SavePref(name = "longs", getter = "getMyLong")
    void saveMyLong(long l);

    @SavePref(name = "bools", getter = "getMyBool")
    void saveMyBool(boolean b);

}
