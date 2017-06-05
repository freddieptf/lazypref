package com.freddieptf.lazyprefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by fred on 6/2/17.
 */
public class LazyPrefManager {

    private static LazyPrefManager INSTANCE = null;
    private Class<?> prefsClass;

    private LazyPrefManager(){
//        throw new IllegalAccessError("Nope! Can't touch this.");
    }

    public static LazyPrefManager get() {
        if(INSTANCE == null) INSTANCE = new LazyPrefManager();
        return INSTANCE;
    }

    public LazyPrefManager init(Class<?> cls){
        String className = cls.getName();
        try {
            prefsClass = Class.forName(className + "_lazy");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }



}
