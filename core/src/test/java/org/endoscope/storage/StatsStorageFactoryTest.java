package org.endoscope.storage;

import org.endoscope.core.Stats;
import org.endoscope.properties.Properties;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.endoscope.core.PropertyTestUtil.withProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StatsStorageFactoryTest {
    public static class TestStorage extends StatsStorage {
        public TestStorage(String initParams){
            super(initParams);
        }

        @Override
        public String save(Stats stats) {
            return "OK";
        }
    }

    @Test
    public void should_create_stats_storage() throws IOException {
        File dir = Files.createTempDirectory("DiskStorageTest").toFile();

        withProperty(Properties.STATS_STORAGE_CLASS, TestStorage.class.getName(), () -> {
        withProperty(Properties.STATS_STORAGE_CLASS_INIT_PARAM, dir.getAbsolutePath(), () -> {
            StatsStorage storage = new StatsStorageFactory().safeCreate();
            assertNotNull(storage);
            assertEquals(Properties.getStatsStorageClass(), storage.getClass().getName());
        });
        });
    }
}