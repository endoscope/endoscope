package com.github.endoscope.storage.gzip;

import com.github.storage.test.StorageTestCases;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GzipStorageTest extends StorageTestCases {

    public GzipStorageTest(){
        super(new GzipStorage());
        try {
            File dir = Files.createTempDirectory("DiskStorageTest").toFile();
            storage.setup(dir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}