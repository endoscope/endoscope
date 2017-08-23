package com.github.endoscope.core;

import java.beans.Transient;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.github.endoscope.properties.Properties;

@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "statsLeft", "lost", "fatalError", "startDate", "endDate", "map" })
public class Stats {
    private Map<String, Stat> map = new HashMap<>();
    private long statsLeft = Properties.getMaxStatCount();
    private AtomicLong lost = new AtomicLong(0);
    private String fatalError = null;
    private Date startDate;
    private Date endDate;
    private String info;

    //do not get it from Properties here as we could loose data by accident by calculating Stats on machine with Property turned off
    private boolean aggregateSubCalls = true;

    public Stats(){}

    /**
     * Set aggregateSubCalls to false to collect entry point's only.
     * It limits Endoscope functionality a bit but significantly reduces amount of data and might be really
     * handy in case of bigger applications.
     *
     * @param aggregateSubCalls
     */
    public Stats(boolean aggregateSubCalls){
        this.aggregateSubCalls = aggregateSubCalls;
    }

    private Stat getOrAddParent(Context context) {
        Stat parentStat = map.get(context.getId());
        if( parentStat == null && statsLeft > 0 ){
            parentStat = new Stat();
            statsLeft--;
            map.put(context.getId(), parentStat);
        }
        return parentStat;
    }

    public void store(Context context){
        store(context, true);
    }

    private void store(Context context, boolean firstPass){
        if( !firstPass && !aggregateSubCalls ){
            return;
        }

        Stat root = getOrAddParent(context);
        if( root != null ){
            root.update(context.getTime());
            store(context, root, firstPass);
        }
    }

    private void store(Context context, final Stat parentStat, boolean firstPass){
        Map<String, Long> subcalls = new HashMap<>();
        if( context.getChildren() != null ){
            //first collect number of calls per parent
            context.getChildren().stream().forEach( child -> {
                Long perParent = subcalls.getOrDefault(child.getId(), 0L) + 1;

                //update child stats
                Stat childStat = parentStat.getChild(child.getId());
                if( childStat == null && statsLeft > 0 ){
                    childStat = parentStat.createChild(child.getId());
                    statsLeft--;
                }
                if( childStat != null ){
                    subcalls.put(child.getId(), perParent);
                    childStat.update(child.getTime());

                    //recurse and update next level of child stats
                    store(child, childStat, firstPass);

                    //stats for child calls are collected in two places:
                    //- in context of parent
                    //- separately as root stats
                    //Context like this:
                    // a -> b -> c
                    //Results in following stats:
                    // a -> b -> c
                    // b -> c
                    // c

                    //recurse and update top level stats for this child
                    if( firstPass ) {
                        store(child, false);
                    }
                }
            });
        }

        if( parentStat.getChildren() != null ){
            parentStat.getChildren().forEach((statId,childStat) ->{
                Long perParent = subcalls.getOrDefault(statId, 0L);
                childStat.updateAvgHits(perParent);
            });
        }
    }

    @Transient
    public Stats deepCopy(){
        return deepCopy(true);
    }

    @Transient
    public Stats deepCopy(boolean withChildren){
        Stats s = new Stats();

        s.statsLeft = statsLeft;
        s.lost.set(lost.get());
        s.fatalError = fatalError;
        s.startDate = startDate;
        s.endDate = endDate;

        map.forEach((k, v) -> s.map.put(k, v.deepCopy(withChildren)));

        return s;
    }

    @Transient
    public void merge(Stats inc, boolean withChildren){
        //too much hassle with merging statsLeft

        lost.set(lost.get() + inc.lost.get());
        if( inc.fatalError != null && fatalError == null ){
            fatalError = inc.fatalError;
        }
        if( startDate == null || (inc.startDate != null && inc.startDate.before(startDate)) ){
            startDate = inc.startDate;
        }
        if( endDate == null || (inc.endDate != null && inc.endDate.after(endDate)) ){
            endDate = inc.endDate;
        }

        inc.map.forEach((k, v) -> {
            Stat s = map.get(k);
            if( s == null ){
                map.put(k, v.deepCopy(withChildren) );
            } else {
                s.merge(v, withChildren);
            }
        });
    }

    /**
     * This method is thread safe.
     */
    @Transient
    public void threadSafeIncrementLost() {
        lost.incrementAndGet();
    }

    public Map<String, Stat> getMap() {
        return map;
    }

    public void setMap(Map<String, Stat> map) {
        this.map = map;
    }

    public long getLost() {
        return lost.get();
    }

    public void setLost(long lost) {
        this.lost.set(lost);
    }

    public long getStatsLeft() {
        return statsLeft;
    }

    public void setStatsLeft(long statsLeft) {
        this.statsLeft = statsLeft;
    }

    public String getFatalError() {
        return fatalError;
    }

    public void setFatalError(String fatalError) {
        this.fatalError = fatalError;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }


    /**
     * Implementation specific information.
     * @return
     */
    public String getInfo() {
        return info;
    }

    /**
     * Implementation specific information.
     * @param info
     */
    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stats)) return false;
        Stats stats = (Stats) o;
        return statsLeft == stats.statsLeft &&
                lost.get() == stats.lost.get() &&
                aggregateSubCalls == stats.aggregateSubCalls &&
                Objects.equals(map, stats.map) &&
                Objects.equals(fatalError, stats.fatalError) &&
                Objects.equals(startDate, stats.startDate) &&
                Objects.equals(endDate, stats.endDate) &&
                Objects.equals(info, stats.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, statsLeft, lost.get(), fatalError, startDate, endDate, info, aggregateSubCalls);
    }
}
