package com.github.endoscope.core;

/**
 * Date: 27/06/2017
 * Time: 14:44
 *
 * @Author p.halicz
 */
public interface AsyncTasksFactory {
    void triggerAsyncTask();
    void stopStatsProcessorThread();
}
