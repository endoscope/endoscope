package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.util.DateUtil;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class StatsCyclicWriter {
    private static final Logger log = getLogger(StatsCyclicWriter.class);

    private String appType;
    private String appInstance;
    private int saveFreqMinutes;
    private Storage storage = null;
    private DateUtil dateUtil;
    private Date lastSave;
    private Date lastError;

    /**
     *
     * @param storage if null then save is disabled
     */
    public StatsCyclicWriter(Storage storage){
        this(storage,
                new DateUtil(),
                Properties.getAppInstance(),
                Properties.getAppType(),
                Properties.getSaveFreqMinutes());
    }

    /**
     *
     * @param storage if null then save is disabled
     * @param dateUtil
     */
    public StatsCyclicWriter(Storage storage, DateUtil dateUtil, String appInstance,
                             String appType, int saveFreqMinutes){
        this.storage = storage;
        this.dateUtil = dateUtil;
        lastSave = dateUtil.now();
        this.appType = appType;
        this.appInstance = appInstance;
        this.saveFreqMinutes = saveFreqMinutes;
    }

    public boolean shouldSave(){
        if( storage != null && saveFreqMinutes > 0 ){
            Date now = dateUtil.now();

            //do not try to save for some time if error occurred
            if( lastError != null ){
                long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - lastError.getTime());
                if( minutes < 5 ){
                    return false;
                }
            }

            long offset = now.getTime() - lastSave.getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(offset);
            return minutes >= saveFreqMinutes;
        }
        return false;
    }

    public void safeSave(Stats stats){
        try{
            if( storage != null ){
                ensureDatesAreSet(stats);
                long start = System.currentTimeMillis();
                storage.save(stats, appInstance, appType);
                lastSave = dateUtil.now();
                lastError = null;
                log.info("Saved stats in {}ms", System.currentTimeMillis() - start);
            }
        }catch(Exception e){
            log.error("failed to save stats - next attempt in 5 minutes", e);
            lastError = dateUtil.now();
        }
    }

    private void ensureDatesAreSet(Stats stats) {
        if( stats.getStartDate() == null ){
            stats.setStartDate(dateUtil.now());
        }
        if( stats.getEndDate() == null ){
            stats.setEndDate(dateUtil.now());
        }
    }

    public Date getLastSaveTime() {
        return lastSave;
    }
}
