    package com.github.endoscope.storage;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.util.DateUtil;
import org.slf4j.Logger;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.slf4j.LoggerFactory.getLogger;

public class StatsPersistence {
    private static final Logger log = getLogger(StatsPersistence.class);

    private String appType;
    private String appInstance;
    private int saveFreqMinutes;
    private Storage storage = null;
    private DateUtil dateUtil;
    private AtomicLong lastSave = new AtomicLong(0);
    private AtomicLong lastError = new AtomicLong(0);
    private int daysToKeep;

    /**
     *
     * @param storage if null then save is disabled
     */
    public StatsPersistence(Storage storage){
        this(storage,
                new DateUtil(),
                Properties.getAppInstance(),
                Properties.getAppType(),
                Properties.getSaveFreqMinutes(),
                Properties.getDaysToKeepData());
    }

    /**
     *
     * @param storage if null then save is disabled
     * @param dateUtil
     */
    public StatsPersistence(Storage storage, DateUtil dateUtil, String appInstance,
                            String appType, int saveFreqMinutes, int daysToKeep){
        this.storage = storage;
        this.dateUtil = dateUtil;
        lastSave.set(dateUtil.now().getTime());
        this.appType = appType;
        this.appInstance = appInstance;
        this.saveFreqMinutes = saveFreqMinutes;
        this.daysToKeep = daysToKeep;
    }

    public boolean threadSafeShouldSave(){
        if( storage != null && saveFreqMinutes > 0 ){
            Date now = dateUtil.now();

            //do not try to save for some time if error occurred
            long val = lastError.get();
            if( val > 0 ){
                long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - val);
                if( minutes < 5 ){
                    return false;
                }
            }

            long offset = now.getTime() - lastSave.get();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(offset);
            return minutes >= saveFreqMinutes;
        }
        return false;
    }

    /**
     * @param stats
     * @return true if successfully saved stats - otherwise false
     */
    public boolean safeSave(Stats stats){
        if( storage == null ){
            return false;
        }
        try{
            ensureDatesAreSet(stats);
            long start = System.currentTimeMillis();
            storage.save(stats, appInstance, appType);
            lastSave.set(dateUtil.now().getTime());
            lastError.set(0);
            log.info("Saved stats in {}ms", System.currentTimeMillis() - start);
            return true;
        }catch(Exception e){
            Throwable cause = firstNonNull(getRootCause(e), e);
            log.warn("Failed to save stats - next attempt in 5 minutes. Error type: {}, Message: {}", cause.getClass().getName(), cause.getMessage());
            log.debug("Failed to save stats - next attempt in 5 minutes. ", e);
            lastError.set(dateUtil.now().getTime());
            return false;
        }
    }

    public void safeCleanup() {
        if( daysToKeep <= 0 || lastError.get() > 0 ){
            return;
        }
        try{
            long start = System.currentTimeMillis();
            storage.cleanup(daysToKeep, appType);
            lastSave.set(dateUtil.now().getTime());
            log.info("Performed cleanup in {}ms", System.currentTimeMillis() - start);
        }catch(Exception e){
            Throwable cause = firstNonNull(getRootCause(e), e);
            log.warn("Failed to cleanup stats. Error type: {}, Message: {}", cause.getClass().getName(), cause.getMessage());
            log.debug("Failed to cleanup stats.", e);
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
        return new Date(lastSave.get());
    }
}
