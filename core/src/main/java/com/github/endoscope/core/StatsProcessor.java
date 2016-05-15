package com.github.endoscope.core;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.StatsCyclicWriter;
import org.slf4j.Logger;

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

    private static int debug_stuff_thread_counter = 0;

    private Stats stats;//will get reset after each save
    private LinkedBlockingDeque<Context> queue;
    private StatsCyclicWriter statsCyclicWriter;
    private ExecutorService collector;

    public StatsProcessor(StatsCyclicWriter statsCyclicWriter) {
        if( statsCyclicWriter == null ){
            throw new IllegalArgumentException("stats cyclic writer cannot be null");
        }

        stats = new Stats();

        queue = new LinkedBlockingDeque<>(Properties.getMaxQueueSize());
        this.statsCyclicWriter = statsCyclicWriter;


        collector = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = Executors.defaultThreadFactory().newThread(runnable);
            t.setDaemon(true);//we don't want to block JVM shutdown
            t.setName("endoscope-stats-collector");
            return t;
        });
        collector.submit(new StatsCollector(this));

        String prop = System.getProperty("endoscope-stats-collector");
        if( prop == null ){
            prop = "1";
        } else {
            long p =Long.valueOf(prop);
            p++;
            prop = ""+p;
        }
        System.setProperty("endoscope-stats-collector", prop);
        debug_stuff_thread_counter++;
        log.info("starting new endoscope-stats-collector thread. Current counter: {}, prop: {}", debug_stuff_thread_counter, prop);
    }

    @Override
    protected void finalize() throws Throwable {
        System.out.println("stoping new endoscope-stats-collector thread. Current counter: " + debug_stuff_thread_counter);
        super.finalize();
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
                stats = new Stats();
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
        log.info("Requested endoscope-stats-collector shutdown");
        collector.shutdownNow();
    }

    public void resetStats(){
        stats = new Stats();
    }
}
