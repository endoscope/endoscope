package org.endoscope.storage;

import org.endoscope.core.Stats;
import org.endoscope.properties.Properties;
import org.endoscope.util.DateUtil;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class StatsCyclicWriter {
    private static final Logger log = getLogger(StatsCyclicWriter.class);

    private int saveFreqMinutes = Properties.getSaveFreqMinutes();
    private StatsStorage statsStorage = null;
    private DateUtil dateUtil;
    private Date lastSave;

    /**
     *
     * @param statsStorage if null then save is disabled
     */
    public StatsCyclicWriter(StatsStorage statsStorage){
        this(statsStorage, new DateUtil());
    }

    /**
     *
     * @param statsStorage if null then save is disabled
     * @param dateUtil
     */
    public StatsCyclicWriter(StatsStorage statsStorage, DateUtil dateUtil){
        this.statsStorage = statsStorage;
        this.dateUtil = dateUtil;
        lastSave = dateUtil.now();
    }

    public boolean shouldSave(){
        if( statsStorage != null && saveFreqMinutes > 0 ){
            Date now = dateUtil.now();
            long offset = now.getTime() - lastSave.getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(offset);
            return minutes >= saveFreqMinutes;
        }
        return false;
    }

    public void safeSave(Stats stats){
        try{
            if( statsStorage != null ){
                ensureDatesAreSet(stats);
                statsStorage.save(stats);
                lastSave = dateUtil.now();
            }
        }catch(Exception e){
            log.error("failed to save stats", e);
        }
    }

    private void ensureDatesAreSet(Stats stats) {
        if( stats.getStartDate() == null ){
            stats.setStartDate(new Date());
        }
        if( stats.getEndDate() == null ){
            stats.setEndDate(new Date());
        }
    }

    public Date getLastSaveTime() {
        return lastSave;
    }
}
