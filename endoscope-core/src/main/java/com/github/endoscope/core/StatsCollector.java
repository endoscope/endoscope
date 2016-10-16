package com.github.endoscope.core;

import com.github.endoscope.util.DebugUtil;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Thread which updates stats with data from queue.
 */
public class StatsCollector implements Runnable {
    private static final Logger log = getLogger(StatsCollector.class);

    private StatsProcessor sp;

    public StatsCollector(StatsProcessor statsProcessor){
        this.sp = statsProcessor;
    }

    @Override
    public void run() {
        log.info("started {}", StatsProcessor.COLLECTOR_THREAD_NAME);
        try{
            while(!Thread.interrupted() && sp.isEnabled() ){
                sp.processAllFromQueue();
                safeSleep();
            }
        }catch(Exception e){
            log.info("stats {} - won't collect any more stats", StatsProcessor.COLLECTOR_THREAD_NAME);
            sp.setFatalError(getStacktrace(e));
        }
        log.info("stopped {}. Count: {}", StatsProcessor.COLLECTOR_THREAD_NAME, DebugUtil.decrementThreadCount());
    }

    private void safeSleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    private String getStacktrace(Exception e) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(buf);
        e.printStackTrace(pw);
        pw.flush();
        return buf.toString();
    }
}
