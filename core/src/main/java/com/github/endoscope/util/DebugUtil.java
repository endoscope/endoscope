package com.github.endoscope.util;

import static com.github.endoscope.core.StatsProcessor.COLLECTOR_THREAD_NAME;

public class DebugUtil {

    public static String incrementThreadCount(){
        //properties is the only thing that survives redeployment on JBoss (static fields don't)
        String prop = System.getProperty(COLLECTOR_THREAD_NAME);
        if( prop == null ){
            prop = "1";
        } else {
            try{
                long p = Long.valueOf(prop);
                p++;
                prop = ""+p;
            }catch(Exception e){
                prop = "666";
            }
        }
        System.setProperty(COLLECTOR_THREAD_NAME, prop);
        return prop;
    }

    public static String decrementThreadCount(){
        //properties is the only thing that survives redeployment on JBoss (static fields don't)
        String prop = System.getProperty(COLLECTOR_THREAD_NAME);
        if( prop != null ){
            try{
                long p = Long.valueOf(prop);
                p--;
                prop = ""+p;
            }catch(Exception e){
                prop = "666";
            }
        }
        System.setProperty(COLLECTOR_THREAD_NAME, prop);
        return prop;
    }
}
