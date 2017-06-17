package com.freddieptf.lazyprefcompiler;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;


/**
 * Created by fred on 6/3/17.
 */

final class PrefMethods {

    static MethodSpec getInt(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return prefs.getInt($S, -1)", pref_key);
        return builder.build();
    }

    static MethodSpec saveInt(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "value")
                .returns(void.class)
                .addStatement("editor.putInt($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getString(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return prefs.getString($S, $S)", pref_key, "");
        return builder.build();
    }

    static MethodSpec saveString(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "value")
                .returns(void.class)
                .addStatement("editor.putString($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getLong(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addStatement("return prefs.getLong($S, -1)", pref_key);
        return builder.build();
    }

    static MethodSpec saveLong(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(long.class, "value")
                .addStatement("editor.putLong($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getFloat(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(float.class)
                .addStatement("return prefs.getFloat($S, -1)", pref_key);
        return builder.build();
    }

    static MethodSpec saveFloat(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(float.class, "value")
                .addStatement("editor.putFloat($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getBoolean(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return prefs.getBoolean($S, $L)", pref_key, false);
        return builder.build();
    }

    static MethodSpec saveBoolean(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(boolean.class, "value")
                .addStatement("editor.putBoolean($S, $N)", pref_key, "value")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getStringSet(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Set.class, String.class))
                .addStatement("return prefs.getStringSet($S, $L)", pref_key, null);
        return builder.build();
    }

    static MethodSpec saveStringSet(String methodName, String pref_key) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Set.class, String.class), "set").build())
                .addStatement("editor.putStringSet($S, $N)", pref_key, "set")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec getObject(String methodName, String pref_key, TypeElement converter, String returnType, String objectType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get(objectType))
                .addStatement("$L converter = new $L()", converter, converter)
                .addStatement("return converter.getVal($L)", genPrefsGet(returnType, pref_key));
        return builder.build();
    }

    static MethodSpec saveObject(String methodName, String pref_key, TypeElement converter, TypeElement returnType, String objectType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .returns(void.class)
                .addParameter(ParameterSpec.builder(TypeVariableName.get(objectType), "val").build())
                .addModifiers(Modifier.PUBLIC)
                .addStatement("$L converter = new $L()", converter, converter)
                .addStatement("$T supportedType = converter.toSupportedType($N)", returnType.asType(), "val")
                .addStatement(genEditorPut(returnType.asType().toString()), pref_key, "supportedType")
                .addStatement("editor.apply()");
        return builder.build();
    }

    static MethodSpec contains() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("contains")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(String.class, "pref_key")
                .addStatement("return prefs.contains($N)", "pref_key");
        return builder.build();
    }

    private static String genEditorPut(String type) {
        if (type.equals(String.class.getCanonicalName())) {
            return "editor.putString($S, $N)";
        } else if (type.equals(int.class.getCanonicalName())) {
            return "editor.putInt($S, $N)";
        } else if (type.equals(float.class.getCanonicalName())) {
            return "editor.putFloat($S, $N)";
        } else if (type.equals(boolean.class.getCanonicalName())) {
            return "editor.putBoolean($S, $N)";
        } else if (type.equals(long.class.getCanonicalName()) || type.equals(Long.class.getCanonicalName())) {
            return "editor.putLong($S, $N)";
        } else {
            return "";
        }
    }

    private static CodeBlock genPrefsGet(String type, String key) {
        if (type.equals(String.class.getCanonicalName())) {
            return CodeBlock.of("prefs.getString($S, $S)", key, "");
        } else if (type.equals(int.class.getCanonicalName()) || type.equals(Integer.class.getCanonicalName())) {
            return CodeBlock.of("prefs.getInt($S, $L)", key, -1);
        } else if (type.equals(float.class.getCanonicalName()) || type.equals(Float.class.getCanonicalName())) {
            return CodeBlock.of("prefs.getFloat($S, $L", key, -1);
        } else if (type.equals(boolean.class.getCanonicalName()) || type.equals(Boolean.class.getCanonicalName())) {
            return CodeBlock.of("prefs.getBoolean($S, $L)", key, false);
        } else if (type.equals(long.class.getCanonicalName()) || type.equals(Long.class.getCanonicalName())) {
            return CodeBlock.of("prefs.getLong($S, $L)", key, -1);
        }
        return null;
    }

}
