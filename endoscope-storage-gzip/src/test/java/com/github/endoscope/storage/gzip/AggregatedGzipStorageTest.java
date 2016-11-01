package com.github.endoscope.storage.gzip;

import com.github.storage.test.AggregatedStorageTestCases;

public class AggregatedGzipStorageTest extends AggregatedStorageTestCases {
    public AggregatedGzipStorageTest(){
        super(new AggregatedGzipStorage());
    }
}