package com.github.endoscope;

import com.github.endoscope.properties.AbstractCustomPropertyProvider;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.gzip.SearchableGzipFileStorage;
import org.slf4j.Logger;

import java.nio.file.Files;

import static org.slf4j.LoggerFactory.getLogger;

public class CustomPropertyProvider extends AbstractCustomPropertyProvider {
    private static final Logger log = getLogger(CustomPropertyProvider.class);

    public CustomPropertyProvider() {
        try {
            override.put(Properties.ENABLED, "true");
            override.put(Properties.STATS_STORAGE_CLASS, SearchableGzipFileStorage.class.getName());
            if( get(Properties.STATS_STORAGE_CLASS_INIT_PARAM, null) == null ){
                String dir = Files.createTempDirectory("endoscope").toFile().getAbsolutePath();
                override.put(Properties.STATS_STORAGE_CLASS_INIT_PARAM, dir);
            }
            log.info("Using storage directory: {}", get(Properties.STATS_STORAGE_CLASS_INIT_PARAM, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
