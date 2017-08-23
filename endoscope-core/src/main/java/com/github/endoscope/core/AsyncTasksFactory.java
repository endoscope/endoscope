package com.github.endoscope.core;

public interface AsyncTasksFactory {
    void triggerAsyncTask();
    void stopStatsProcessorThread();
}
