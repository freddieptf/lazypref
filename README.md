# lazyprefs
Code generation for your SharedPreferences util classes.

## Usage

Create an interface and annotate it with @LazyPref annotation. Then add any fields you'd like to store in your shared preferences and annotate them with the @Pref annotation

```java
// you can provide a preferenceName here if you don't want to
// use the default shared prefences
// @LazyPref(preferenceName = "me_nums_nums")
@LazyPref
public interface SharedPrefs {
    @Pref(key = "nums")
    int number = 0;
}
```

Recompile your project and a class will be generated in the same package with the suffix `_lazy`

```java
public final class SharedPrefs_lazy {
  private static SharedPrefs_lazy INSTANCE;

  private final SharedPreferences prefs;

  private final SharedPreferences.Editor editor;

  private SharedPrefs_lazy(final Context context) {
    this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    editor = this.prefs.edit();
  }

  public static SharedPrefs_lazy getInstance(Context context) {
    if (INSTANCE == null) INSTANCE = new SharedPrefs_lazy(context);
    return INSTANCE;
  }

  public boolean contains(String pref_key) {
    return prefs.contains(pref_key);
  }

  public void saveNumber(int value) {
    editor.putInt("nums", value);
    editor.apply();
  }

  public int getNumber() {
    return prefs.getInt("nums", -1);
  }

}

```

### The @LazyPref Annotation

```java
public @interface LazyPref {

    String preferenceName() default ""; // Name of the preference file to be created

}
```

### The @Pref Annotation

```java
public @interface Pref {
    String key() default ""; // if no key is provided, we use the variable name as the key

    boolean autoGenGet() default true; // autogenerates a getter by defaut

    Class<? extends TypeConverter> converter() default TypeConverter.class; // for when we want to save an unsupported type
}
```

### Type Converters
There is support for type converters for types not supported by shared preferences. You only need to implement the TypeConverter interface for your own converters

```java
public interface TypeConverter<T, E> {
    E toSupportedType(T val);

    T getVal(E val);
}
```

Example: A type converter to converter a pojo to a string and vice versa

```java
public class UserPrefConverter implements TypeConverter<User, String> {
    public UserPrefConverter() { }

    @Override
    public String toSupportedType(User val) {
        String s = val.name + "-" + val.id;
        return s;
    }

    @Override
    public User getVal(String val) {
        String[] strings = val.split("-");
        return new User(strings[0], strings[1]);
    }
}
```
and finally
```java
    ...
    @Pref(converter = UserPrefConverter.class)
    User primaryUser = null;
    ...
```

### Including it to your app via gradle

```
allprojects {
	repositories {
		jcenter()
	}
}
```

```
dependencies {
    compile 'com.freddieptf:lazypref-annotations:0.1.2'    
    annotationProcessor 'com.freddieptf:lazypref-compiler:0.1.2'
}
```


### License
The MIT License (MIT)
```
Copyright (c) 2017 Fred Muiru

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
