package com.github.endoscope.storage.aggr;

import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.Storage;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.ceiling;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static org.slf4j.LoggerFactory.getLogger;

public class AggregatedStorage implements com.github.endoscope.storage.AggregatedStorage {
    private static final Logger log = getLogger(AggregatedStorage.class);

    private static final long MINUTE_LENGTH_MS = 60 * 1000;
    private static final long DAY_LENGTH_MS = 24 * 60 * MINUTE_LENGTH_MS;
    private static final long WEEK_LENGTH_MS = 7 * DAY_LENGTH_MS;
    private static final long MONTH_LENGTH_MS = 30 * DAY_LENGTH_MS;

    protected Storage defaultStorage;
    protected Storage dailyStorage;
    protected Storage weeklyStorage;
    protected Storage monthlyStorage;

    private boolean aggregateOnly = false;

    @Override
    public void setStorage(Storage defaultStorage, Storage dailyStorage, Storage weeklyStorage, Storage monthlyStorage) {
        this.defaultStorage = defaultStorage;
        this.dailyStorage = dailyStorage;
        this.weeklyStorage = weeklyStorage;
        this.monthlyStorage = monthlyStorage;
    }

    /**
     * When true then saves aggregated stats only. For example when you want to create aggregated stats for already
     * existing stats.
     * @return
     */
    public boolean isAggregateOnly() {
        return aggregateOnly;
    }

    @Override
    public void setAggregateOnly(boolean aggregateOnly) {
        this.aggregateOnly = aggregateOnly;
    }

    @Override
    public void setup(String initParam) {
        //this is general version that doesn't setup anything
        if( defaultStorage == null ){
            throw new IllegalStateException("Storage not setup properly");
        }
    }

    @Override
    public String save(Stats stats, String instance, String type) {
        validateStartDate(stats);

        String result = null;
        if( !isAggregateOnly()){
            result = defaultStorage.save(stats, instance, type);
        }

        //We group stats per type for better performance - hence we loose instance information in following stats
        updateAggregated(dailyStorage, Calendar.DAY_OF_MONTH, stats, type);
        updateAggregated(weeklyStorage, Calendar.WEEK_OF_YEAR, stats, type);
        updateAggregated(monthlyStorage, Calendar.MONTH, stats, type);

        return result;
    }

    @Override
    public String replace(String statsId, Stats stats, String instance, String type) {
        throw new RuntimeException("Operation not supported");//there is no way to un-merge stats
    }

    private Calendar cal(Date d){
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTime(d);
        return calendar;
    }

    private void updateAggregated(Storage storage, int timeUnit, Stats stats, String type) {
        Date start, end;
        if(timeUnit == Calendar.WEEK_OF_YEAR){
            Calendar c = cal(stats.getStartDate());
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            start = truncate(c, Calendar.DAY_OF_MONTH).getTime();
            end = addSeconds(DateUtils.addDays(start, 7), -1);//a second before next stats
        } else {
            start = truncate(cal(stats.getStartDate()), timeUnit).getTime();
            end   = addSeconds(ceiling(cal(stats.getStartDate()), timeUnit).getTime(), -1);//a second before next stats
        }

        //search second before and second after to avoid round problems
        List<String> ids = storage.find(addSeconds(start, 1), addSeconds(end, -1), null, type);
        String replaceId = null;
        Stats aggregated;
        if( ids.isEmpty() ){
            aggregated = new Stats();
        } else {
            if( ids.size() > 0 ){
                log.warn("Found more than one aggregated stat to update!");
            }
            replaceId = ids.get(0);
            aggregated = storage.load(replaceId);
        }

        aggregated.merge(stats, true);

        //Merged stats are matched to aggregation period by start date only. Because of that end day might exceed
        // end date of aggregated stats and would update it during merge operation - we need to restore original dates.
        aggregated.setStartDate( start );
        aggregated.setEndDate( end );

        //Possible race condition when multiple threads try to update aggregated stats at the same time
        storage.replace(replaceId, aggregated, null, type);
    }

    private void validateStartDate(Stats stats) {
        if( stats.getStartDate() == null ){
            throw new IllegalStateException("Start date cannot be null");
        }
    }

    @Override
    public Stats load(String groupId) {
        return defaultStorage.load(groupId);
    }

    private Storage chooseStorage(Date from, Date to){
        long periodLength = to.getTime() - from.getTime() + MINUTE_LENGTH_MS;

        if( periodLength < DAY_LENGTH_MS ){
            return defaultStorage;
        }
        if( periodLength < WEEK_LENGTH_MS ){
            return dailyStorage;
        }
        if( periodLength < MONTH_LENGTH_MS ){
            return weeklyStorage;
        }
        return monthlyStorage;
    }

    /**
     * In aggregated stats we don't keep this information.
     * We need to reset it in order to find values in that storage.
     *
     * @param storage
     * @param instance
     * @return
     */
    private String fixInstance(Storage storage, String instance){
        return defaultStorage == storage ? instance : null;
    }

    @Override
    public List<String> find(Date from, Date to, String instance, String type) {
        Storage storage = chooseStorage(from, to);
        instance = fixInstance(storage, instance);
        return storage.find(from, to, instance, type);
    }

    @Override
    public Filters findFilters(Date from, Date to, String type) {
        Storage storage = chooseStorage(from, to);
        return storage.findFilters(from, to, type);
    }

    @Override
    public StatDetails loadDetails(String detailsId, List<String> groupIds) {
        throw new RuntimeException("Operation not supported");//there is no way to identify storage without time period
    }

    @Override
    public StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type) {
        Storage storage = chooseStorage(from, to);
        instance = fixInstance(storage, instance);
        return storage.loadDetails(detailsId, from, to, instance, type);
    }

    @Override
    public Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type) {
        Storage storage = chooseStorage(from, to);
        instance = fixInstance(storage, instance);
        return storage.loadAggregated(topLevelOnly, from, to, instance, type);
    }
}
