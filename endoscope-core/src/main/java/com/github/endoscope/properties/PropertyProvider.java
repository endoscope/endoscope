package com.github.endoscope.properties;

public interface PropertyProvider {
    String IMPLEMENTATION_CLASS_NAME = "com.github.endoscope.CustomPropertyProvider";

    String get(String name, String defaultValue);
}
