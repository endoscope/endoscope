package com.github.endoscope.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.endoscope.storage.StatsPersistence;
import com.github.endoscope.storage.Storage;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class CurrentStatsAsyncTasks implements AsyncTasksFactory {
    private static final Logger log = getLogger(CurrentStatsAsyncTasks.class);
    public static final String COLLECTOR_THREAD_NAME = "endoscope-stats-collect";
    public static final String SAVING_THREAD_NAME = "endoscope-stats-save";
    public static final String COLLECTOR_ID = UUID.randomUUID().toString();
    public static final String SAVING_ID = UUID.randomUUID().toString();

    private ExecutorService collectingExecutor;
    private ExecutorService savingExecutor;
    private CurrentStats currentStats;
    private StatsPersistence statsPersistence;
    private boolean enabled = true;
    private Future collectingTaskResult;
    private Future savingTaskResult;

    public CurrentStatsAsyncTasks(CurrentStats currentStats, Storage storage) {
        this.currentStats = currentStats;
        this.statsPersistence = new StatsPersistence(storage);

        collectingExecutor = new ThreadPoolExecutor(0, 1,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setDaemon(true);//we don't want to block JVM shutdown
                    t.setName(COLLECTOR_THREAD_NAME);
                    return t;
        });

        savingExecutor = new ThreadPoolExecutor(0, 1,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setDaemon(true);//we don't want to block JVM shutdown
                    t.setName(SAVING_THREAD_NAME);
                    return t;
                });
    }

    public void triggerAsyncTask() {
        try{
            if (!enabled || (collectingTaskResult != null && !collectingTaskResult.isDone())) {
                //previous task is still running
                //it's not a perfect synchronization but it doesn't have to be
                return;
            }
            log.debug("Creating new async task for collector: {}", COLLECTOR_ID);
            collectingTaskResult = collectingExecutor.submit(() -> {

                //this stuff runs in new thread

                safeSleep();
                log.debug("started async task for collector: {}", COLLECTOR_ID);
                try {
                    currentStats.processAllFromQueue();
                    if (statsPersistence != null && statsPersistence.threadSafeShouldSave()) {
                        //run save in another thread so we don't block processing elements from queue
                        triggerAsyncSafeSave();
                    }
                } catch (Exception e) {
                    currentStats.setFatalError(getStacktrace(e));
                    log.debug("error occurred when processing async task for collector: {}", COLLECTOR_ID, e);
                }
                log.debug("finished async task for collector: {}", COLLECTOR_ID);
            });
            log.debug("Created new async task for collector: {}", COLLECTOR_ID);
        }catch(Exception e){
            log.warn("Failed to trigger async task for collector: {}", COLLECTOR_ID, e);
        }
    }

    private void triggerAsyncSafeSave() {
        try{
            if (!enabled ) {
                return;
            }
            if (savingTaskResult != null && !savingTaskResult.isDone()) {
                log.debug("Previous async task for saving stats: {} is still running...skipping", SAVING_ID);
                //it's not a perfect synchronization but it doesn't have to be
                return;
            }
            log.debug("Creating new async task for saving stats: {}", SAVING_ID);
            savingTaskResult = savingExecutor.submit(() -> {

                //this stuff runs in new thread

                safeSleep();
                log.debug("started async task for saving stats: {}", SAVING_ID);
                try {
                    safeSave();
                } catch (Exception e) {
                    log.debug("error occurred when processing async task for saving stats: {}", SAVING_ID, e);
                }
                log.debug("finished async task for saving stats: {}", SAVING_ID);
            });
            log.debug("Created new async task for saving stats: {}", SAVING_ID);
        }catch(Exception e){
            log.warn("Failed to trigger async task for saving stats: {}", SAVING_ID, e);
        }
    }

    private boolean safeSave() {
        //this stuff runs in new thread

        log.debug("persisting stats");

        //don't lock for long here! - just get stats we plan to save and start to collect new values
        Stats oldStats = currentStats.lockReadStats(stats -> currentStats.resetStats() );

        boolean saved = statsPersistence.safeSave(oldStats);
        if( !saved ){
            log.debug("failed to save stats - returning stats in order to try again later");
            currentStats.lockReadStats(stats -> {
                stats.merge(oldStats, true);
                return true;
            });
        } else {
            log.debug("saved stats - running stats cleanup");
            statsPersistence.safeCleanup();
        }

        log.debug("finished persisting stats");
        return true;
    }

    public void stopStatsProcessorThread() {
        log.info("Requested threads: {}, {} shutdown", COLLECTOR_THREAD_NAME, SAVING_THREAD_NAME);
        enabled = false;
        collectingExecutor.shutdownNow();
        savingExecutor.shutdownNow();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static void safeSleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    private static String getStacktrace(Exception e) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(buf);
        e.printStackTrace(pw);
        pw.flush();
        return buf.toString();
    }
}
