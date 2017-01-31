package com.github.endoscope.core;

import com.github.endoscope.storage.StatsPersistence;
import com.github.endoscope.storage.Storage;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class CurrentStatsAsyncTasks {
    private static final Logger log = getLogger(CurrentStatsAsyncTasks.class);
    public static final String COLLECTOR_THREAD_NAME = "endoscope-stats-collector";
    public static final String COLLECTOR_ID = UUID.randomUUID().toString();

    private ExecutorService collector;
    private CurrentStats currentStats;
    private StatsPersistence statsPersistence;
    private boolean enabled = true;
    private Future taskResult;

    public CurrentStatsAsyncTasks(CurrentStats currentStats, Storage storage) {
        this.currentStats = currentStats;
        this.statsPersistence = new StatsPersistence(storage);

        collector = new ThreadPoolExecutor(0, 1,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setDaemon(true);//we don't want to block JVM shutdown
                    t.setName(COLLECTOR_THREAD_NAME);
                    return t;
        });
    }

    public void triggerAsyncTask() {
        try{
            if (!enabled || (taskResult != null && !taskResult.isDone())) {
                //previous task is still running
                //it's not a perfect synchronization but it doesn't have to be
                return;
            }
            log.debug("Creating new async task: {}", COLLECTOR_ID);
            taskResult = collector.submit(() -> {
                safeSleep();
                log.debug("started async task: {}", COLLECTOR_ID);
                try {
                    currentStats.processAllFromQueue();
                    //following might take some time
                    if( safeSaveIfNeeded() ){
                        //so just before existing update in-memory stats again
                        currentStats.processAllFromQueue();
                    }
                } catch (Exception e) {
                    currentStats.setFatalError(getStacktrace(e));
                    log.debug("error occurred when processing async task: {}", COLLECTOR_ID, e);
                }
                log.debug("finished async task: {}", COLLECTOR_ID);
            });
            log.debug("Created new async task: {}", COLLECTOR_ID);
        }catch(Exception e){
            log.warn("Failed to trigger asynchronous stats collection task", e);
        }
    }

    private boolean safeSaveIfNeeded() {
        if (statsPersistence != null && statsPersistence.shouldSave()) {
            log.debug("persisting stats: {}", COLLECTOR_ID);
            currentStats.readStats(stats -> {
                statsPersistence.safeSave(stats);
                currentStats.resetStats();
                return null;
            });
            log.debug("running stats cleanup: {}", COLLECTOR_ID);
            statsPersistence.safeCleanup();
            log.debug("finished persisting stats: {}", COLLECTOR_ID);
            return true;
        }
        return false;
    }

    public void stopStatsProcessorThread() {
        log.info("Requested {} shutdown", COLLECTOR_THREAD_NAME);
        enabled = false;
        collector.shutdownNow();
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
