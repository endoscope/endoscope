package com.github.endoscope.storage.gzip;

import com.github.endoscope.storage.aggr.AggregatedStorage;

public class AggregatedGzipStorage extends AggregatedStorage {
    @Override
    public void setup(String dirName) {
        defaultStorage  = new GzipStorage();
        defaultStorage.setup(dirName);

        dailyStorage    = new GzipStorage();
        dailyStorage.setup(dirName + "/day");

        weeklyStorage   = new GzipStorage();
        weeklyStorage.setup(dirName + "/week");

        monthlyStorage  = new GzipStorage();
        monthlyStorage.setup(dirName + "/month");
    }
}
