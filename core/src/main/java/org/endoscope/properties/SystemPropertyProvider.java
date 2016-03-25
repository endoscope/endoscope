package org.endoscope.properties;

public class SystemPropertyProvider implements PropertyProvider {
    @Override
    public String get(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }
}
