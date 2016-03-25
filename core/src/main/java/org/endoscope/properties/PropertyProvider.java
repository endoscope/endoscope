package org.endoscope.properties;

public interface PropertyProvider {
    String get(String name, String defaultValue);
}
