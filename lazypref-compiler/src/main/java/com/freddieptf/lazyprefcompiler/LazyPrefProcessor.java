package com.freddieptf.lazyprefcompiler;

import com.freddieptf.lazyprefannotations.LazyPref;
import com.freddieptf.lazyprefannotations.Pref;
import com.freddieptf.lazyprefannotations.TypeConverter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
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

    private static String getKlasName(TypeElement type, String packageName) {
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
        roundEnvironment.getElementsAnnotatedWith(LazyPref.class)
                .stream()
                .map(TypeElement.class::cast)
                .filter(e -> {
                    boolean b = e.getKind() == ElementKind.INTERFACE;
                    if (!b)
                        writeError(e, "Only Interfaces can be annotated with @%s", LazyPref.class.getSimpleName());
                    return b;
                })
                .forEach(this::processLazyPrefInterface);
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>() {{
            add(Pref.class.getCanonicalName());
            add(LazyPref.class.getCanonicalName());
        }};
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void writeError(Element e, String message, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(message, args),
                e);
    }

    private void writeWarning(Element e, String message, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(message, args),
                e);
    }

    private String getPkgName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private boolean sanityFilter(Element childElement) {
        if (childElement.getKind() == ElementKind.FIELD) {
            List annotations = elementUtils.getAllAnnotationMirrors(childElement);
            if (annotations.size() <= 0) {
                writeWarning(childElement, "Some fields with no annotations were found. Did you forget to annotate them?");
            } else if (annotations.size() > 1) {
                writeError(childElement, "You can't have more than one annotation on a field");
                return true;
            }
        } else {
            writeError(childElement, "Only variables/fields can be annotated with @%s! Found a %s", Pref.class.getSimpleName(), childElement.getKind());
            return true;
        }
        return false;
    }

    private boolean processLazyPrefInterface(TypeElement element) {
        String pkgName = getPkgName(element);
        String className = getKlasName(element, pkgName) + LazyBaby.LAZY_SUFFIX;
        String preferenceName = element.getAnnotation(LazyPref.class).preferenceName();
        LazyBaby.Builder builder = new LazyBaby.Builder(pkgName, className)
                .setPreferenceName(preferenceName)
                .buildClass();
        // sanity check needed? idk..
        element.getEnclosedElements()
                .parallelStream()
                .forEach(this::sanityFilter);

        builder = addMethodSpecsToBuilder(element, builder);
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
        return false;
    }

    private LazyBaby.Builder addMethodSpecsToBuilder(TypeElement typeElement, LazyBaby.Builder lazyBabyBuilder) {
        typeElement.getEnclosedElements().forEach(element -> {
            Pref annotation = element.getAnnotation(Pref.class);
            List<MethodSpec> methodSpecs = generateRequiredPrefMethods(element, annotation);
            if (methodSpecs.isEmpty()) {
                writeWarning(element, "Could not generate methods for this element");
            } else {
                lazyBabyBuilder.addMethods(methodSpecs);
            }
        });
        return lazyBabyBuilder;
    }

    private List<MethodSpec> generateRequiredPrefMethods(Element element, Pref annotation) {
        String type = element.asType().toString();
        String varName = element.getSimpleName().toString();
        String capVarName = varName.substring(0, 1).toUpperCase() + varName.substring(1);
        String prefKey = annotation.key().isEmpty() ? varName : annotation.key();
        List<MethodSpec> methodSpecs = new ArrayList<>();

        if (element.asType().getKind().isPrimitive() || type.equals(String.class.getCanonicalName())
                || type.equals(ParameterizedTypeName.get(Set.class, String.class).toString())) {
            methodSpecs = processSupportedType(annotation, type, varName, capVarName, prefKey);
        } else {
            methodSpecs = processUnsupportedType(element, annotation, capVarName, prefKey);
        }

        return methodSpecs;
    }


    private List<MethodSpec> processSupportedType(Pref annotation, String type, String simpleVarName, String capitalizedVarName, String prefKey) {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        if (type.equals(int.class.getCanonicalName())) {
            methodSpecs.add(PrefMethods.saveInt("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getInt("get" + capitalizedVarName, prefKey));
            }
        } else if (type.equals(String.class.getCanonicalName())) {
            methodSpecs.add(PrefMethods.saveString("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getString("get" + capitalizedVarName, prefKey));
            }
        } else if (type.equals(float.class.getCanonicalName())) {
            methodSpecs.add(PrefMethods.saveFloat("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getFloat("get" + capitalizedVarName, prefKey));
            }
        } else if (type.equals(boolean.class.getCanonicalName())) {
            methodSpecs.add(PrefMethods.saveBoolean("set" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getBoolean(simpleVarName, prefKey));
            }
        } else if (type.equals(long.class.getCanonicalName())) {
            methodSpecs.add(PrefMethods.saveLong("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getLong("get" + capitalizedVarName, prefKey));
            }
        } else if (type.equals(ParameterizedTypeName.get(Set.class, String.class).toString())) {
            methodSpecs.add(PrefMethods.saveStringSet("save" + capitalizedVarName, prefKey));
            if (annotation.autoGenGet()) {
                methodSpecs.add(PrefMethods.getStringSet("get" + capitalizedVarName, prefKey));
            }
        }
        return methodSpecs;
    }

    private List<MethodSpec> processUnsupportedType(Element element, Pref annotation, String capVarName, String prefKey) {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        try {
            annotation.converter();
        } catch (MirroredTypeException e) {
            e.printStackTrace();
            TypeMirror typeMirror = e.getTypeMirror();
            TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
            if (typeElement.getSimpleName().toString().equals(TypeConverter.class.getSimpleName())) {
                writeError(element, "No converter provided for unsupported type. You must provide it human! hint:'@Pref(converter=Class<? extends TypeConverter>)'");
                return new ArrayList<>();
            }
            typeElement.getEnclosedElements()
                    .stream()
                    .filter(enclosedElement -> enclosedElement.getKind() == ElementKind.METHOD)
                    .forEach(enclosedElement -> {
                        ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                        if (executableElement.getSimpleName().toString().equals("toSupportedType")) {
                            methodSpecs.add(PrefMethods.saveObject(
                                    "save" + capVarName,
                                    prefKey,
                                    typeElement,
                                    (TypeElement) typeUtils.asElement(executableElement.getReturnType()),
                                    element.asType().toString()));
                        } else if (executableElement.getSimpleName().toString().equals("getVal") && annotation.autoGenGet()) {
                            methodSpecs.add(PrefMethods.getObject(
                                    "get" + capVarName,
                                    prefKey,
                                    typeElement,
                                    executableElement.getParameters().get(0).asType().toString(),
                                    element.asType().toString()));
                        }
                    });
        }
        return methodSpecs;
    }

    private boolean createHelperClass(String packageName) {
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
