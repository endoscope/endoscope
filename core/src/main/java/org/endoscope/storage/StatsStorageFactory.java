package org.endoscope.storage;

import org.endoscope.properties.Properties;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class StatsStorageFactory {
    private static final Logger log = getLogger(StatsStorageFactory.class);

    /**
     * Should not fail. May return null in case of failure.
     * @return
     */
    public StatsStorage safeCreate(){
        String className = Properties.getStatsStorageClass();
        String classInitParam = Properties.getStatsStorageClassInitParam();

        if( className != null && className.length() > 0 ){
            try {
                Class<? extends StatsStorage> clazz = (Class<? extends StatsStorage>)Class.forName(className);
                StatsStorage storage = clazz.getConstructor(String.class).newInstance(classInitParam);
                log.info("Successfully created StatsStorage instance: {}, with params: {}", className, classInitParam);
                return storage;
            } catch (Exception e) {
                log.warn("Failed to create new StatsStorage: {}, with params: {}.", className, classInitParam, e);
            }
        }
        return null;
    }
}
