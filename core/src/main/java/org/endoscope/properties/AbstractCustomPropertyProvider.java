package org.endoscope.properties;

import java.util.HashMap;
import java.util.Map;

public class AbstractCustomPropertyProvider extends SystemPropertyProvider {
    protected Map<String, String> override = new HashMap<>();

    @Override
    public String get(String name, String defaultValue) {
        String value = override.get(name);
        return value != null ? value : super.get(name, defaultValue);
    }
}
