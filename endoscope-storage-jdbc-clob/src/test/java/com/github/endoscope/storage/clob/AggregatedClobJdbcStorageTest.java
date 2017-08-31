package com.github.endoscope.storage.clob;

import com.github.storage.test.AggregatedStorageTestCases;

public class AggregatedClobJdbcStorageTest extends AggregatedStorageTestCases {
    public AggregatedClobJdbcStorageTest(){
        super(new AggregatedClobJdbcStorage());
    }
}