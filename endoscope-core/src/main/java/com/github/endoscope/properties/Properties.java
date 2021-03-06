package com.github.endoscope.properties;

import com.github.endoscope.util.AppIdentificationUtil;

public class Properties {
    public static String MAX_STAT_COUNT = "endoscope.max-stat-count";
    public static String QUEUE_MAX_SIZE = "endoscope.max-queue-size";
    public static String EXCLUDED_PACKAGES = "endoscope.excluded-packages";
    public static String SUPPORTED_NAMES = "endoscope.supported-names";

    //true/false
    public static String ENABLED = "endoscope.enabled";

    public static String SCANNED_PACKAGES = "endoscope.scanned-packages";

    //After successful save memory statistics get's cleaned
    public static String SAVE_FREQ_MINUTES = "endoscope.save-feq-minutes";//set to <= 0 in order to disable
    public static String STORAGE_CLASS = "endoscope.storage-class";
    public static String STORAGE_CLASS_INIT_PARAM = "endoscope.storage-class-init-param";
    public static String MAX_ID_LENGTH = "endoscope.max-id-length";
    public static String DEV_RESOURCES_DIR = "endoscope.dev-res-dir";
    public static String APP_TYPE = "endoscope.app-type";
    public static String APP_INSTANCE = "endoscope.app-instance";
    public static String DEPRECATED_APP_GROUP = "endoscope.app-group";//now it's instance
    public static String AGGREGATE_SUB_CALLS = "endoscope.aggregate-sub-calls";
    public static String DAYS_TO_KEEP = "endoscope.days-to-keep";//set <= 0 to disable

    /*
     Credentials format is: "username:password"
     By default it works with endoscope exposed at /endoscope/* path.
     If you use different path configure com.github.endoscope.cdiui.SecurityFilter in web.xml with your own settings.
    */
    public static String AUTH_CREDENTIALS = "endoscope.auth-credentials";

    public static String DEFAULT_MAX_STAT_COUNT = "300000";
    public static String DEFAULT_SUPPORTED_NAMES = ".*(Bean|Service|Controller|Ejb|EJB)";
    public static String DEFAULT_QUEUE_MAX_SIZE = "100000";//it's number of Context trees in queue so overall size depends on app but it's something like: 100k -> ~100MB
    public static String DEFAULT_SAVE_FREQ_MINUTES = "15";
    public static String DEFAULT_MAX_ID_LENGTH = "100";
    public static String DEFAULT_AGGREGATE_SUB_CALLS = "true";
    public static String DEFAULT_DAYS_TO_KEEP = "35";



    private static final PropertyProvider propertyProvider = PropertyProviderFactory.create();

    private static String safeGetProperty(String name, String defaultValue){
        try{
            String value = propertyProvider.get(name, null);
            if (value == null){
                //Fallback to old names (not so friendly for YAML)
                //it used to be:        endoscope.storage.class.init.param
                //while new version is: endoscope.storage-class-init-param

                String oldStyleName = name.replaceAll("-", ".");
                value = propertyProvider.get(oldStyleName, defaultValue);
            }
            if( value != null ){
                value = value.trim();
            }
            return value;
        }catch(Exception e){
        }
        return defaultValue;
    }

    public static boolean isEnabled(){
        return "true".equalsIgnoreCase(safeGetProperty(ENABLED, "false"));
    }

    public static String[] getScannedPackages(){
        return safeGetProperty(SCANNED_PACKAGES, "").split(",");
    }

    public static String[] getPackageExcludes(){
        return safeGetProperty(EXCLUDED_PACKAGES, "").split(",");
    }

    public static String getSupportedNames(){
        return safeGetProperty(SUPPORTED_NAMES, DEFAULT_SUPPORTED_NAMES);
    }

    public static long getMaxStatCount(){

// RAM usage (com.github.endoscope.impl.StatsTest.estimate_stats_size)
//        100000 ~ 13 MB
//        200000 ~ 28 MB
//        300000 ~ 42 MB << default
//        400000 ~ 59 MB
//        500000 ~ 73 MB
//        600000 ~ 88 MB
//        700000 ~ 102 MB
//        800000 ~ 121 MB
//        900000 ~ 135 MB
//        1000000 ~ 150 MB
//JSON size (com.github.endoscope.impl.StatsTest.estimate_json_stats_size):
//        100000 ~ 6 MB
//        200000 ~ 13 MB
//        300000 ~ 19 MB << default (~1MB when compressed)
//        400000 ~ 26 MB
//        500000 ~ 32 MB
//        600000 ~ 39 MB
//        700000 ~ 45 MB
//        800000 ~ 52 MB
//        900000 ~ 59 MB
//        1000000 ~ 65 MB
        return Long.valueOf(safeGetProperty(MAX_STAT_COUNT, DEFAULT_MAX_STAT_COUNT));//~42MB
    }

    public static int getMaxQueueSize(){
        return Integer.valueOf(safeGetProperty(QUEUE_MAX_SIZE, DEFAULT_QUEUE_MAX_SIZE));
    }

    public static int getSaveFreqMinutes(){
        return Integer.valueOf(safeGetProperty(SAVE_FREQ_MINUTES, DEFAULT_SAVE_FREQ_MINUTES));
    }

    public static String getStorageClassInitParam(){
        return safeGetProperty(STORAGE_CLASS_INIT_PARAM, null);
    }

    public static String getStorageClass(){
        return safeGetProperty(STORAGE_CLASS, null);
    }

    public static int getMaxIdLength(){
        return Integer.valueOf(safeGetProperty(MAX_ID_LENGTH, DEFAULT_MAX_ID_LENGTH));
    }

    public static String getDevResourcesDir(){
        return safeGetProperty(DEV_RESOURCES_DIR, null);
    }

    public static String getAppInstance(){
        //backward compatibility
        String value = safeGetProperty(DEPRECATED_APP_GROUP, null);
        if( value != null){
            return value;
        }

        return safeGetProperty(APP_INSTANCE, AppIdentificationUtil.calculateHost());
    }

    public static String getAppType(){
        return safeGetProperty(APP_TYPE, AppIdentificationUtil.calculateType());
    }

    public static String getAuthCredentials() {
        return safeGetProperty(AUTH_CREDENTIALS, null);
    }

    public static boolean getAggregateSubCalls(){
        //notice that: "true".equalsIgnoreCase behaves in different way in case of incorrect value
        //we wan't it disabled when set to "false" (whatever character case) only.
        return !"false".equalsIgnoreCase(safeGetProperty(AGGREGATE_SUB_CALLS, DEFAULT_AGGREGATE_SUB_CALLS));
    }

    public static int getDaysToKeepData() {
        return Integer.valueOf(safeGetProperty(DAYS_TO_KEEP, DEFAULT_DAYS_TO_KEEP));
    }
}
