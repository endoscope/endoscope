package com.github.endoscope.storage;

import com.github.endoscope.properties.Properties;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class StorageFactory {
    private static final Logger log = getLogger(StorageFactory.class);

    /**
     * Should not fail. May return null in case of failure.
     * @return
     */
    public Storage safeCreate(){
        String className = Properties.getStorageClass();
        String initParam = Properties.getStorageClassInitParam();

        if( className != null && className.length() > 0 ){
            try {
                Class<? extends Storage> clazz = (Class<? extends Storage>)Class.forName(className);
                Storage storage = clazz.newInstance();
                storage.setup(initParam);
                log.debug("Successfully created StatsStorage instance: {}, with params: {}", className, initParam);
                return storage;
            } catch (Exception e) {
                log.warn("Failed to create new StatsStorage: {}, with params: {}.", className, initParam, e);
            }
        } else {
            log.debug("Storage class not specified");
        }
        return null;
    }
}
