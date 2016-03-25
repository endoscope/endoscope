package com.github.endoscope.properties;

public interface PropertyProvider {
    static final String IMPLEMENTATION_CLASS_NAME = "com.github.endoscope.CustomPropertyProvider";

    String get(String name, String defaultValue);
}
