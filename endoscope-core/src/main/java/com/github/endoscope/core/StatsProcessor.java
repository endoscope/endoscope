package com.github.endoscope.core;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.StatsCyclicWriter;
import com.github.endoscope.util.DebugUtil;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class is responsible for thread safe operations on stats and queue.
 * All operations on this class must be thread safe.
 *
 * Storing contexts in queue must be thread safe and as fast as possible.
 * This is critical requirement as it would affect monitored application performance.
 *
 * Operations on stats also needs to be thread safe as we don't know who runs #process method.
 * All stats operations might take a lot of time: save, processing and updates (in case of complex stats).
 * Because of that we need different thread that will move data from queue to stats.
 */
public class StatsProcessor {
    private static final Logger log = getLogger(StatsProcessor.class);
    public static String COLLECTOR_THREAD_NAME = "endoscope-stats-collector";

    private Stats stats;//will get reset after each save
    private LinkedBlockingDeque<Context> queue;
    private StatsCyclicWriter statsCyclicWriter;
    private ExecutorService collector;
    private boolean enabled = true;

    public StatsProcessor(StatsCyclicWriter statsCyclicWriter) {
        if( statsCyclicWriter == null ){
            throw new IllegalArgumentException("stats cyclic writer cannot be null");
        }

        stats = createEmptyStats();

        queue = new LinkedBlockingDeque<>(Properties.getMaxQueueSize());
        this.statsCyclicWriter = statsCyclicWriter;


        collector = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);//we don't want to block JVM shutdown
            t.setName(COLLECTOR_THREAD_NAME);
            return t;
        });
        collector.submit(new StatsCollector(this));

        log.info("Created new {} thread. Current count: {}", COLLECTOR_THREAD_NAME, DebugUtil.incrementThreadCount());
    }

    private Stats createEmptyStats() {
        Stats stats = new Stats(Properties.getAggregateSubCalls());
        stats.setStartDate(new Date());
        return stats;
    }

    public void store(Context context){
        try{
            queue.addLast(context);
        }catch(IllegalStateException e){
            synchronized(stats){
                stats.incrementLost();
            }
        }
    }

    public <T> T process(Function<Stats, T> function){
        synchronized(stats){
            return function.apply(stats);
        }
    }

    public int getQueueSize(){
        return queue.size();
    }

    private void safeSaveIfNeeded(){
        if( statsCyclicWriter.shouldSave() ){
            synchronized(stats){
                statsCyclicWriter.safeSave(stats);
                statsCyclicWriter.safeCleanup();
                stats = createEmptyStats();
            }
        }
    }

    //internal use - accessed from processing thread
    void processAllFromQueue(){
        Context ctx = queue.poll();
        synchronized(stats){
            while(ctx != null){
                stats.store(ctx);
                ctx = queue.pollFirst();
            }
        }
        safeSaveIfNeeded();
    }

    //internal use - accessed from processing thread
    void setFatalError(String message){
        stats.setFatalError(message);//assignment is thread safe
    }

    public void stopStatsProcessorThread(){
        log.info("Requested {} shutdown", COLLECTOR_THREAD_NAME);
        enabled = false;
        collector.shutdownNow();
    }

    public boolean isEnabled(){
        return enabled;
    }

    public void resetStats(){
        stats = createEmptyStats();
    }
}
