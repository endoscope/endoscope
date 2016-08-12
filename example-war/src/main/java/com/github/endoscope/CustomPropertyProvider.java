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
            setNx(Properties.ENABLED, "true");
            //setNx(Properties.AUTH_CREDENTIALS, "user:password");

            String storageClass = setNx(Properties.STATS_STORAGE_CLASS_INIT_PARAM, SearchableGzipFileStorage.class.getName());
            String storageParam = setNx(Properties.STATS_STORAGE_CLASS_INIT_PARAM, Files.createTempDirectory("endoscope").toFile().getAbsolutePath());
            log.info("Using storage: {} with parameter: {}{}", storageClass, storageParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
