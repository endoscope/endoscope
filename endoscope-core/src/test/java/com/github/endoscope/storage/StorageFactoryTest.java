package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import static com.github.endoscope.util.PropertyTestUtil.withProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StorageFactoryTest {
    @Test
    public void should_create_stats_storage() throws IOException {
        File dir = Files.createTempDirectory("DiskStorageTest").toFile();

        withProperty(Properties.STORAGE_CLASS, TestStorage.class.getName(), () -> {
        withProperty(Properties.STORAGE_CLASS_INIT_PARAM, dir.getAbsolutePath(), () -> {
            Storage storage = new StorageFactory().safeCreate();
            assertNotNull(storage);
            assertEquals(Properties.getStorageClass(), storage.getClass().getName());
        });
        });
    }

    public static class TestStorage implements Storage {
        @Override
        public void setup(String initParam) {

        }

        @Override
        public String save(Stats stats, String instance, String type) {
            return null;
        }

        @Override
        public String replace(String id, Stats stats, String instance, String type) {
            return null;
        }

        @Override
        public Stats load(String id) {
            return null;
        }

        @Override
        public List<String> find(Date from, Date to, String instance, String type) {
            return null;
        }

        @Override
        public Filters findFilters(Date from, Date to, String type) {
            return null;
        }

        @Override
        public StatDetails loadDetails(String detailsId, List<String> stats) {
            return null;
        }

        @Override
        public StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type) {
            return null;
        }

        @Override
        public Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type) {
            return null;
        }
    }
}