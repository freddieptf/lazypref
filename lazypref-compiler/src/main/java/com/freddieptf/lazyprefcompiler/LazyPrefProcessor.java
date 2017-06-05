package com.freddieptf.lazyprefcompiler;

import com.freddieptf.lazyprefannotations.GetPref;
import com.freddieptf.lazyprefannotations.LazyPref;
import com.freddieptf.lazyprefannotations.SavePref;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ParameterizedTypeName;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by fred on 5/28/17.
 */
@AutoService(Processor.class)
public class LazyPrefProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // map<classname, classbuilder>
        Map<String, LazyBaby.Builder> lazyBabyMap = new HashMap<>();

        for(Element element : roundEnvironment.getElementsAnnotatedWith(LazyPref.class)){
            if(element.getKind() != ElementKind.INTERFACE){
                writeError(element, "Only Interfaces can be annotated with @%s", LazyPref.class.getSimpleName());
                return true;
            }
            String pkgName = getPackageName((TypeElement) element);
            String className = getClassName((TypeElement) element, pkgName) + LazyBaby.LAZY_SUFFIX;
            LazyBaby.Builder builder = new LazyBaby.Builder(pkgName, className)
                    .buildClass();
            lazyBabyMap.put(className, builder);
            // sanity check needed? idk..
            for(Element childElement : ((TypeElement)element).getEnclosedElements()){
                if(childElement.getKind() == ElementKind.METHOD){
                    List annotations = elementUtils.getAllAnnotationMirrors(childElement);
                    if(annotations.size() <= 0){
                        writeWarning(childElement, "methods with no annotations were found. Did you forget something?");
                    } else if (annotations.size() > 1) {
                        writeError(childElement, "can't have more than one annotation on a method");
                        return true;
                    }
                }
            }
        }

        for(Element element : roundEnvironment.getElementsAnnotatedWith(GetPref.class)){
            if(element.getKind() != ElementKind.METHOD){
                writeError(element, "Only methods can be annotated with @%s", GetPref.class.getSimpleName());
                return true;
            }
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            if(enclosingElement.getKind() != ElementKind.INTERFACE){
                writeError(element, "Annotated methods can only be within an interface");
                return true;
            }
            String pkgName = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, pkgName) + LazyBaby.LAZY_SUFFIX;
            LazyBaby.Builder lazyBabyBuilder = lazyBabyMap.get(className);

            if(lazyBabyBuilder == null) lazyBabyBuilder = new LazyBaby.Builder(pkgName, className).buildClass();

            ExecutableElement executableElement = (ExecutableElement) element;
            GetPref annotation = element.getAnnotation(GetPref.class);
            lazyBabyBuilder = getGetterTypes(executableElement, lazyBabyBuilder, annotation);
            lazyBabyMap.put(className, lazyBabyBuilder);
        }


        for(Element element : roundEnvironment.getElementsAnnotatedWith(SavePref.class)){
            if(element.getKind() != ElementKind.METHOD){
                writeError(element, "Only methods can be annotated with @%s", SavePref.class.getSimpleName());
                return true;
            }
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            if(enclosingElement.getKind() != ElementKind.INTERFACE) {
                writeError(element, "Annotated methods can only be within an interface");
                return true;
            }
            String pkgName = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, pkgName) + LazyBaby.LAZY_SUFFIX;
            LazyBaby.Builder lazyBabyBuilder = lazyBabyMap.get(className);

            if(lazyBabyBuilder == null) lazyBabyBuilder = new LazyBaby.Builder(pkgName, className).buildClass();

            ExecutableElement executableElement = (ExecutableElement) element;
            if(executableElement.getParameters().isEmpty()){
                writeError(element, "You must provide the argument to be saved in %s method!",  element.getSimpleName());
                return true;
            }
            SavePref annotation = element.getAnnotation(SavePref.class);
            lazyBabyBuilder = getSetterTypes(executableElement, lazyBabyBuilder, annotation);
            lazyBabyMap.put(className, lazyBabyBuilder);
        }

        for(LazyBaby.Builder lazyBabyBuilder : lazyBabyMap.values()) {
            try {
                lazyBabyBuilder.build().generateSource(filer);
            } catch (NullPointerException | IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(){{
            add(GetPref.class.getCanonicalName());
            add(SavePref.class.getCanonicalName());
        }};
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void writeError(Element e, String message, Object ...args){
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(message, args),
                e);
    }

    private void writeWarning(Element e, String message, Object ...args){
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(message, args),
                e);
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private LazyBaby.Builder getGetterTypes(ExecutableElement element, LazyBaby.Builder builder, GetPref annotation){
        String type = element.getReturnType().toString();
        if(type.equals(int.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getInt(element.getSimpleName().toString(), annotation.name()));
        } else if(type.equals(String.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getString(element.getSimpleName().toString(), annotation.name()));
        } else if(type.equals(long.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getLong(element.getSimpleName().toString(), annotation.name()));
        } else if(type.equals(boolean.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getBoolean(element.getSimpleName().toString(), annotation.name()));
        } else if(type.equals(float.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getFloat(element.getSimpleName().toString(), annotation.name()));
        } else if(type.contains(Set.class.getCanonicalName())){
            builder.addMethod(PrefMethods.getStringSet(element.getSimpleName().toString(), annotation.name()));
        }
        return builder;
    }

    private LazyBaby.Builder getSetterTypes(ExecutableElement element, LazyBaby.Builder builder, SavePref annotation){
        VariableElement variableElement = element.getParameters().get(0);
        String type = variableElement.asType().toString();
        System.out.printf("save %s %s\n", type, 22);
        if(type.equals(int.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveInt(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getInt(annotation.getter(), annotation.name()));
            }
        } else if(type.equals(String.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveString(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getString(annotation.getter(), annotation.name()));
            }
        } else if(type.equals(float.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveFloat(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getFloat(annotation.getter(), annotation.name()));
            }
        } else if(type.equals(boolean.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveBoolean(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getBoolean(annotation.getter(), annotation.name()));
            }
        } else if(type.equals(long.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveLong(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getLong(annotation.getter(), annotation.name()));
            }
        } else if(type.contains(Set.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveStringSet(element.getSimpleName().toString(), annotation.name()));
            if(!annotation.getter().isEmpty()) {
                builder.addMethod(PrefMethods.getStringSet(annotation.getter(), annotation.name()));
            }
        }
        return builder;
    }
}
