package com.github.endoscope;

import java.nio.file.Files;

import com.github.endoscope.properties.AbstractCustomPropertyProvider;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.gzip.GzipStorage;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class CustomPropertyProvider extends AbstractCustomPropertyProvider {
    private static final Logger log = getLogger(CustomPropertyProvider.class);

    public CustomPropertyProvider() {
        try {
            setNx(Properties.ENABLED, "true");
            //setNx(Properties.AUTH_CREDENTIALS, "user:password");

            String storageClass = setNx(Properties.STORAGE_CLASS, GzipStorage.class.getName());
            String storageParam = setNx(Properties.STORAGE_CLASS_INIT_PARAM, Files.createTempDirectory("endoscope").toFile().getAbsolutePath());
            log.info("Using storage: {} with parameter: {}{}", storageClass, storageParam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
