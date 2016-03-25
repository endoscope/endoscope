package org.endoscope.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class StatsCollector implements Runnable {
    private static final Logger log = getLogger(StatsCollector.class);

    private StatsProcessor sp;

    public StatsCollector(StatsProcessor statsProcessor){
        this.sp = statsProcessor;
    }

    @Override
    public void run() {
        log.info("started stats collector thread");
        try{
            while(!Thread.interrupted()){
                sp.processAllFromQueue();
                safeSleep();
            }
        }catch(Exception e){
            log.info("stats collector thread interrupted - won't collect any more stats");
            sp.setFatalError(getStacktrace(e));
        }
        log.info("stopped stats collector thread");
    }

    private void safeSleep() {
        try {
            Thread.sleep(10);
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
