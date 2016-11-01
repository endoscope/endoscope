package com.github.endoscope.storage;

public interface AggregatedStorage extends Storage {
    void setStorage(Storage defaultStorage, Storage dailyStorage, Storage weeklyStorage, Storage monthlyStorage);
    void setAggregateOnly(boolean aggregateOnly);
}