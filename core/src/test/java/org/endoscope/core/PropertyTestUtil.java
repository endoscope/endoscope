package org.endoscope.core;

public class PropertyTestUtil {

    public static void withProperty(String name, String value, Runnable runnable) {
        String previousValue = System.getProperty(name);
        System.setProperty(name, value);
        try{
            runnable.run();
        }finally{
            if( previousValue == null ){
                System.clearProperty(name);
            } else {
                System.setProperty(name, previousValue);
            }
        }
    }
}
