package com.freddieptf.lazyprefcompiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

/**
 * Created by fred on 6/2/17.
 */

final class LazyBaby {

    static String LAZY_SUFFIX = "_lazy";

    private final String classPkgName;
    private final TypeSpec.Builder classBuilder;

    private LazyBaby(String classPkgName, TypeSpec.Builder classBuilder) {
        this.classPkgName = classPkgName;
        this.classBuilder = classBuilder;
    }

    void generateSource(Filer filer) throws IOException{
        JavaFile javaFile = JavaFile.builder(classPkgName, classBuilder.build()).build();
        javaFile.writeTo(filer);
    }

    static class Builder {
        private final String packageName;
        private final String className;
        private String preferenceName;
        private TypeSpec.Builder classBuilder;
        private List<MethodSpec> methods = new ArrayList<>();

        Builder(String packageName, String className){
            this.packageName = packageName;
            this.className = className;
        }

        Builder buildClass(){
            ClassName generatedClassName = ClassName.get(packageName, className);
            ClassName sharedPreferencesClassName = ClassName.get("android.content", "SharedPreferences");
            ClassName contextClassName = ClassName.get("android.content", "Context");
            ClassName preferenceManagerClassName = ClassName.get("android.preference", "PreferenceManager");

            FieldSpec prefs = FieldSpec.builder(sharedPreferencesClassName, "prefs", Modifier.FINAL, Modifier.PRIVATE)
                    .build();

            FieldSpec editor = FieldSpec.builder(sharedPreferencesClassName.nestedClass("Editor"), "editor", Modifier.FINAL, Modifier.PRIVATE)
                    .build();

            FieldSpec instance = FieldSpec.builder(generatedClassName, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC)
                    .build();

            MethodSpec getter = MethodSpec.methodBuilder("getInstance")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(contextClassName, "context").build())
                    .returns(ClassName.get(packageName, className))
                    .addStatement("if (INSTANCE == null) INSTANCE = new $T($N)", generatedClassName, "context")
                    .addStatement("return INSTANCE")
                    .build();

            MethodSpec defaultConstructor;

            if (preferenceName.isEmpty())
                defaultConstructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(contextClassName, "context", Modifier.FINAL)
                        .addStatement("this.$N = $T.getDefaultSharedPreferences($N)", "prefs", preferenceManagerClassName, "context")
                        .addStatement("editor = this.$N.edit()", "prefs")
                        .build();
            else
                defaultConstructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(contextClassName, "context", Modifier.FINAL)
                        .addStatement("this.$N = $N.getSharedPreferences($S, $T.MODE_PRIVATE)", "prefs", "context", preferenceName, contextClassName)
                        .addStatement("editor = this.$N.edit()", "prefs")
                        .build();

            classBuilder = TypeSpec.classBuilder(className)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addField(instance)
                    .addField(prefs)
                    .addField(editor)
                    .addMethod(defaultConstructor)
                    .addMethod(getter)
                    .addMethod(PrefMethods.contains());

            return this;
        }

        Builder setPreferenceName(String preferenceName) {
            this.preferenceName = preferenceName;
            return this;
        }

        Builder addMethod(MethodSpec methodSpec) {
            methods.add(methodSpec);
            return this;
        }

        Builder addMethods(List<MethodSpec> methodSpecs) {
            methods.addAll(methodSpecs);
            return this;
        }

        LazyBaby build(){
            classBuilder.addMethods(methods);
            return new LazyBaby(packageName, classBuilder);
        }

    }

}
