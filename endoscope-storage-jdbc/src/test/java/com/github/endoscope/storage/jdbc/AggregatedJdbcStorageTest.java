package com.github.endoscope.storage.jdbc;

import com.github.storage.test.AggregatedStorageTestCases;

public class AggregatedJdbcStorageTest extends AggregatedStorageTestCases {
    public AggregatedJdbcStorageTest(){
        super(new AggregatedJdbcStorage());
    }
}