package com.freddieptf.sample.converter;

import com.freddieptf.lazyprefannotations.TypeConverter;

/**
 * Created by fred on 6/8/17.
 */

public class IntArrayConverter implements TypeConverter<int[], String> {

    public IntArrayConverter() {
    }

    @Override
    public String toSupportedType(int[] val) {
        String s = "";
        for (int i : val) s = s.concat(i + " ");
        return s;
    }

    @Override
    public int[] getVal(String val) {
        String[] sa = val.split(" ");
        int[] ia = new int[sa.length];
        for (int i = 0; i < sa.length; i++) {
            ia[i] = Integer.parseInt(sa[i]);
        }
        return ia;
    }
}
