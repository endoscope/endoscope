package org.endoscope.properties;

public class Properties {
    public static String MAX_STAT_COUNT = "endoscope.max.stat.count";
    public static String QUEUE_MAX_SIZE = "endoscope.max.queue.size";
    public static String EXCLUDED_PACKAGES = "endoscope.excluded-packages";
    public static String SUPPORTED_NAMES = "endoscope.supported-names";
    public static String ENABLED = "endoscope.enabled";
    public static String SCANNED_PACKAGES = "endoscope.scanned-packages";
    public static String SAVE_FREQ_MINUTES = "endoscope.save.feq.minutes";//set to <= 0 in order to disable
    public static String STATS_STORAGE_CLASS = "endoscope.storage.class";
    public static String STATS_STORAGE_CLASS_INIT_PARAM = "endoscope.storage.class.init.param";
    public static String MAX_ID_LENGTH = "endoscope.max.id.length";

    public static String DEFAULT_MAX_STAT_COUNT = "300000";
    public static String DEFAULT_SUPPORTED_NAMES = ".*(Bean|Service|Controller|Ejb|EJB)";
    public static String DEFAULT_QUEUE_MAX_SIZE = "1000000";
    public static String DEFAULT_SAVE_FREQ_MINUTES = "5";
    public static String DEFAULT_MAX_ID_LENGTH = "100";



    private static final PropertyProvider propertyProvider = PropertyProviderFactory.create();

    private static String safeGetProperty(String name, String defaultValue){
        try{
            String value = propertyProvider.get(name, defaultValue);
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

// RAM usage (org.endoscope.impl.StatsTest.estimate_stats_size)
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
//JSON size (org.endoscope.impl.StatsTest.estimate_json_stats_size):
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

    public static String getStatsStorageClassInitParam(){
        return safeGetProperty(STATS_STORAGE_CLASS_INIT_PARAM, null);
    }

    public static String getStatsStorageClass(){
        return safeGetProperty(STATS_STORAGE_CLASS, null);
    }

    public static int getMaxIdLength(){
        return Integer.valueOf(safeGetProperty(MAX_ID_LENGTH, DEFAULT_MAX_ID_LENGTH));
    }
}
