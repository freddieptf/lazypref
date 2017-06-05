package com.freddieptf.lazyprefcompiler;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;


/**
 * Created by fred on 6/3/17.
 */

public class PrefMethods {

    public static MethodSpec getInt(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return prefs.getInt($S, -1)", pref_key);
        return builder.build();
    }

    public static MethodSpec saveInt(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "value")
                .returns(void.class)
                .addStatement("editor.putInt($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec getString(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return prefs.getString($S, $S)", pref_key, "");
        return builder.build();
    }

    public static MethodSpec saveString(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "value")
                .returns(void.class)
                .addStatement("editor.putString($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec getLong(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addStatement("return prefs.getLong($S, -1)", pref_key);
        return builder.build();
    }

    public static MethodSpec saveLong(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(long.class, "value")
                .addStatement("editor.putLong($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec getFloat(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(float.class)
                .addStatement("return prefs.getFloat($S, -1)", pref_key);
        return builder.build();
    }

    public static MethodSpec saveFloat(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(float.class, "value")
                .addStatement("editor.putFloat($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec getBoolean(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return prefs.getBoolean($S, $L)", pref_key, false);
        return builder.build();
    }

    public static MethodSpec saveBoolean(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(boolean.class, "value")
                .addStatement("editor.putBoolean($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec getStringSet(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Set.class, String.class))
                .addStatement("return prefs.getStringSet($S, $L)", pref_key, null);
        return builder.build();
    }

    public static MethodSpec saveStringSet(String methodName, String pref_key){
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Set.class, String.class), "set").build())
                .addStatement("editor.putStringSet($S, $N)", pref_key, "set")
                .addStatement("editor.apply()");
        return builder.build();
    }

    public static MethodSpec contains(){
        MethodSpec.Builder builder = MethodSpec.methodBuilder("contains")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(String.class, "pref_key")
                .addStatement("return prefs.contains($N)", "pref_key");
        return builder.build();
    }


}
