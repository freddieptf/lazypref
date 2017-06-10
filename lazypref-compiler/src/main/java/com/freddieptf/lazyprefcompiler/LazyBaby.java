package com.freddieptf.lazyprefcompiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

/**
 * Created by fred on 6/2/17.
 */

public class LazyBaby {

    static String LAZY_SUFFIX = "_lazy";

    final String classPkgName;
    final String className;
    final TypeSpec.Builder classBuilder;

    public LazyBaby(String classPkgName, String className, TypeSpec.Builder classBuilder){
        this.classPkgName = classPkgName;
        this.className = className;
        this.classBuilder = classBuilder;
    }

    void generateSource(Filer filer) throws IOException{
        JavaFile javaFile = JavaFile.builder(classPkgName, classBuilder.build()).build();
        javaFile.writeTo(filer);
    }

    static class Builder {
        private final String packageName;
        private final String className;
        private TypeSpec.Builder classBuilder;
        private List<MethodSpec> methods = new ArrayList<>();

        Builder(String packageName, String className){
            this.packageName = packageName;
            this.className = className;
        }

        Builder buildClass(){
            ClassName generatedClassName = ClassName.get(packageName, className);
            ClassName sharedPreferencesClassName = ClassName.get("android.content", "SharedPreferences");

            FieldSpec prefs = FieldSpec
                    .builder(TypeVariableName.get("SharedPreferences"), "prefs", Modifier.FINAL, Modifier.PRIVATE)
                    .build();

            FieldSpec editor = FieldSpec
                    .builder(TypeVariableName.get("SharedPreferences.Editor"), "editor", Modifier.FINAL, Modifier.PRIVATE)
                    .build();

            FieldSpec instance = FieldSpec.builder(generatedClassName, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC)
                    .build();

            MethodSpec getter = MethodSpec.methodBuilder("getInstance")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(sharedPreferencesClassName, "prefs").build())
                    .returns(ClassName.get(packageName, className))
                    .addStatement("if (INSTANCE == null) INSTANCE = new $T($N)", generatedClassName, "prefs")
                    .addStatement("return INSTANCE")
                    .build();

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(sharedPreferencesClassName, "prefs", Modifier.FINAL)
                    .addStatement("this.$N = $N", "prefs", "prefs")
                    .addStatement("editor = this.$N.edit()", "prefs")
                    .build();

            classBuilder = TypeSpec.classBuilder(className)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addField(instance)
                    .addField(prefs)
                    .addField(editor)
                    .addMethod(constructor)
                    .addMethod(getter)
                    .addMethod(PrefMethods.contains());

            return this;
        }

        Builder addMethod(MethodSpec builder){
            methods.add(builder);
            return this;
        }

        LazyBaby build(){
            classBuilder.addMethods(methods);
            return new LazyBaby(packageName, className, classBuilder);
        }

    }

}
