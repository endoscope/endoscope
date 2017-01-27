package com.github.endoscope.core;

import com.github.endoscope.properties.Properties;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class contains current Stats and queue of Contexts awaiting to be added to current stats.
 * It is responsible for separation of quick synchronous operations from longer asynchronous operations.
 *
 * All operations on this class must be thread safe.
 *
 * Storing Contexts in queue must be thread safe and as fast as possible.
 * This is critical requirement as otherwise it could affect monitored application performance.
 *
 * Operations on stats also needs to be thread safe as we don't know who runs #readStats method.
 * All stats operations might take a lot of time: save, processing and updates (in case of complex stats).
 * Because of that we need different thread that will move data from queue to stats.
 */
public class CurrentStats {
    private static final Logger log = getLogger(CurrentStats.class);

    private Stats stats;
    private LinkedBlockingDeque<Context> queue;

    public CurrentStats() {
        stats = createEmptyStats();
        queue = new LinkedBlockingDeque<>(Properties.getMaxQueueSize());
    }

    private Stats createEmptyStats() {
        Stats stats = new Stats(Properties.getAggregateSubCalls());
        stats.setStartDate(new Date());
        return stats;
    }

    public void add(Context context){
        try{
            queue.addLast(context);
        }catch(IllegalStateException e){//eg. due to exhausted queue size
            synchronized(stats){
                stats.incrementLost();
            }
        }
    }

    public <T> T readStats(Function<Stats, T> function){
        synchronized(stats){
            return function.apply(stats);
        }
    }

    public int getQueueSize(){
        return queue.size();
    }

    void processAllFromQueue(){
        Context ctx = queue.pollFirst();
        synchronized(stats){
            while(ctx != null){
                stats.store(ctx);
                ctx = queue.pollFirst();
            }
        }
    }

    public void setFatalError(String message){
        stats.setFatalError(message);//assignment is thread safe
    }

    public void resetStats(){
        stats = createEmptyStats();
    }
}
