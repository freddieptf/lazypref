package com.freddieptf.lazyprefcompiler;

import com.freddieptf.lazyprefannotations.LazyPref;
import com.freddieptf.lazyprefannotations.Pref;
import com.freddieptf.lazyprefannotations.TypeConverter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
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
    // TODO: 7/23/17 figure out how to get the app's packageName
    private String pkgNameForHelper = "";

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

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

        for(Element element : roundEnvironment.getElementsAnnotatedWith(LazyPref.class)){
            if(element.getKind() != ElementKind.INTERFACE){
                writeError(element, "Only Interfaces can be annotated with @%s", LazyPref.class.getSimpleName());
                return true;
            }
            String pkgName = getPackageName((TypeElement) element);
            String className = getClassName((TypeElement) element, pkgName) + LazyBaby.LAZY_SUFFIX;
            LazyBaby.Builder builder = new LazyBaby.Builder(pkgName, className)
                    .buildClass();
            // sanity check needed? idk..
            for(Element childElement : ((TypeElement)element).getEnclosedElements()){
                if (childElement.getKind() == ElementKind.FIELD) {
                    List annotations = elementUtils.getAllAnnotationMirrors(childElement);
                    if(annotations.size() <= 0){
                        writeWarning(childElement, "Some fields with no annotations were found. Did you forget to annotate them?");
                    } else if (annotations.size() > 1) {
                        writeError(childElement, "You can't have more than one annotation on a field");
                        return true;
                    }
                }
            }
            builder = genLazyPrefClass((TypeElement) element, builder);
            if (builder == null) return true;
            try {
                builder.build().generateSource(filer);
            } catch (IOException e) {
                e.printStackTrace();
                writeError(element, "Failed while trying to generate source");
                return true;
            }
            // this will do for now until we figure out how to get the app's package name
            if (pkgNameForHelper.isEmpty()) {
                pkgNameForHelper = pkgName;
                createHelperClass(pkgNameForHelper);
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(){{
            add(Pref.class.getCanonicalName());
            add(LazyPref.class.getCanonicalName());
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

    private LazyBaby.Builder genLazyPrefClass(TypeElement typeElement, LazyBaby.Builder lazyBabyBuilder) {
        if (typeElement.getKind() != ElementKind.INTERFACE) {
            writeError(typeElement, "Annotated methods can only be within an interface");
            return null;
        }
        String pkgName = getPackageName(typeElement);
        String className = getClassName(typeElement, pkgName) + LazyBaby.LAZY_SUFFIX;
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.FIELD) {
                writeError(element, "Only variables/fields can be annotated with @%s! Found a %s", Pref.class.getSimpleName(), element.getKind());
                return null;
            }
            if (lazyBabyBuilder == null)
                lazyBabyBuilder = new LazyBaby.Builder(pkgName, className).buildClass();
            Pref annotation = element.getAnnotation(Pref.class);
            lazyBabyBuilder = generateRequiredPrefMethods(element, lazyBabyBuilder, annotation);
            if (lazyBabyBuilder == null) {
                writeError(element, "Code generation failed.");
                return null;
            }
        }
        return lazyBabyBuilder;
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private LazyBaby.Builder generateRequiredPrefMethods(Element element, LazyBaby.Builder builder, Pref annotation) {
        if (!addMethodToLazyBabyBuilder(element, builder, annotation))
            return null;
        return builder;
    }

    private boolean addMethodToLazyBabyBuilder(Element element, LazyBaby.Builder builder, Pref annotation) {
        String type = element.asType().toString();
        String varName = element.getSimpleName().toString();
        String capVarName = varName.substring(0, 1).toUpperCase() + varName.substring(1);
        String prefKey = annotation.key().isEmpty() ? varName : annotation.key();

        if (element.asType().getKind().isPrimitive() || type.equals(String.class.getCanonicalName())
                || type.equals(ParameterizedTypeName.get(Set.class, String.class).toString())) {
            processSupportedType(builder, annotation, type, varName, capVarName, prefKey);
        } else {
            if (!processUnsupportedType(element, builder, annotation, capVarName, prefKey))
                return false;
        }
        return true;
    }

    private void processSupportedType(LazyBaby.Builder builder, Pref annotation, String type, String simpleVarName, String capitalizedVarName, String prefKey) {
        if(type.equals(int.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveInt("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getInt("get" + capitalizedVarName, prefKey));
            }
        } else if(type.equals(String.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveString("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getString("get" + capitalizedVarName, prefKey));
            }
        } else if(type.equals(float.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveFloat("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getFloat("get" + capitalizedVarName, prefKey));
            }
        } else if(type.equals(boolean.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveBoolean("set" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getBoolean(simpleVarName, prefKey));
            }
        } else if(type.equals(long.class.getCanonicalName())){
            builder.addMethod(PrefMethods.saveLong("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getLong("get" + capitalizedVarName, prefKey));
            }
        } else if (type.equals(ParameterizedTypeName.get(Set.class, String.class).toString())) {
            builder.addMethod(PrefMethods.saveStringSet("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                builder.addMethod(PrefMethods.getStringSet("get" + capitalizedVarName, prefKey));
            }
        }
    }

    private boolean processUnsupportedType(Element element, LazyBaby.Builder builder, Pref annotation, String capVarName, String prefKey) {
        try {
            annotation.converter();
        } catch (MirroredTypeException e) {
            e.printStackTrace();
            TypeMirror typeMirror = e.getTypeMirror();
            TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
            if (typeElement.getSimpleName().toString().equals(TypeConverter.class.getSimpleName())) {
                writeError(element, "No converter provided for unsupported type. You must provide it human! hint:'@Pref(converter=Class<? extends TypeConverter>)'");
                return false;
            }
            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD) {
                    ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                    if (executableElement.getSimpleName().toString().equals("toSupportedType")) {
                        builder.addMethod(PrefMethods.saveObject(
                                "save" + capVarName,
                                prefKey,
                                typeElement,
                                (TypeElement) typeUtils.asElement(executableElement.getReturnType()),
                                element.asType().toString()));
                    } else if (executableElement.getSimpleName().toString().equals("getVal") && annotation.autoGenGet()) {
                        builder.addMethod(PrefMethods.getObject(
                                "get" + capVarName,
                                prefKey,
                                typeElement,
                                executableElement.getParameters().get(0).asType().toString(),
                                element.asType().toString()));
                    }
                }
            }
        }
        return true;
    }

    boolean createHelperClass(String packageName) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("LazyPreferenceHelper")
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addMethods(PrefMethods.getHelperMethods());
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
        try {
            javaFile.writeTo(filer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
