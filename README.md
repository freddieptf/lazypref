# lazyprefs
Simple Code generation for your SharedPreferences util classes.

# Usage

Well..see..*here's some code*

```java
@LazyPref
public interface SharedPrefs {

    @Pref(key = "nums")
    int number = 0;
    
    // support for typeconverters
    @Pref(converter = IntArrayConverter.class)
    int[] ageArray = new int[]{};
    ... 
}
```

...and here's the generated class.

```java
public final class SharedPrefs_lazy {
  private static SharedPrefs_lazy INSTANCE;
  private final SharedPreferences prefs;
  private final SharedPreferences.Editor editor;

  private SharedPrefs_lazy(final SharedPreferences prefs) {
    this.prefs = prefs;
    editor = this.prefs.edit();
  }

  public static SharedPrefs_lazy getInstance(SharedPreferences prefs) {
    if (INSTANCE == null) INSTANCE = new SharedPrefs_lazy(prefs);
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

  public void saveAgeArray(int[] val) {
    com.freddieptf.sample.converter.IntArrayConverter converter = new com.freddieptf.sample.converter.IntArrayConverter();
    String supportedType = converter.toSupportedType(val);
    editor.putString("ageArray", supportedType);
    editor.apply();
  }

  public int[] getAgeArray() {
    com.freddieptf.sample.converter.IntArrayConverter converter = new com.freddieptf.sample.converter.IntArrayConverter();
    return converter.getVal(prefs.getString("ageArray", ""));
  }
}
```

This is still in development, ***also is this readme***, any PRs/Reviews welcome.


