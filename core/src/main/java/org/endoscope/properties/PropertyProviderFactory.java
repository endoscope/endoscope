package org.endoscope.properties;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class PropertyProviderFactory {
    private static final Logger log = getLogger(PropertyProviderFactory.class);

    public static PropertyProvider create(){
        Class c = null;
        try {
            //no service discovery - it's just a hack
            //properties are an elegant solution if you don't want to use non project packe in your code
            c = Class.forName("org.endoscope.CustomPropertyProvider");
        } catch (ClassNotFoundException e) {
        }
        PropertyProvider pp = null;
        if( c != null ){
            try {
                pp = (PropertyProvider)c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.warn("Found CustomPropertyProvider but failed to instantiate it: {}", e.getMessage());
            }
        }
        if( pp != null ){
            log.info("Using CustomPropertyProvider");
            return pp;
        } else {
            log.info("Using deafult SystemPropertyProvider");
            return new SystemPropertyProvider();
        }
    }
}
