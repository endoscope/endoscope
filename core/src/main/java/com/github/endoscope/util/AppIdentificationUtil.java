package com.github.endoscope.util;

import java.net.InetAddress;
import java.net.URL;

public class AppIdentificationUtil {
    public static String calculateHost(){
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch(Exception e){
            return "unknown_host";
        }
    }

    public static String calculateType(){
        try{
            String resource = AppIdentificationUtil.class.getName().replaceAll("\\.", "/") + ".class";
            URL url = ClassLoader.getSystemClassLoader().getResource(resource);
            String group = url.getFile().replace("/" + resource, "");
            return group;
        } catch(Exception e){
            return "unknown_group";
        }
    }
}
