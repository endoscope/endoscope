package org.endoscope.core;

import org.endoscope.properties.Properties;

import java.beans.Transient;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "statsLeft", "lost", "fatalError", "startDate", "endDate", "map" })
public class Stats {
    private Map<String, Stat> map = new HashMap<>();
    private long statsLeft = Properties.getMaxStatCount();
    private long lost = 0;
    private String fatalError = null;
    private Date startDate;
    private Date endDate;

    public Stats(){
        startDate = new Date();
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
        Stat root = getOrAddParent(context);
        if( root != null ){
            root.update(context.getTime());
            store(context, root, firstPass);
        }
    }

    private void store(Context context, final Stat parentStat, boolean firstPass){
        if( context.getChildren() != null ){
            //first collect number of calls per parent
            Map<String, Long> subcalls = new HashMap<>();
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

                    //recurse and update top level stats
                    if( firstPass ) {
                        store(child, false);
                    }
                }
            });

            subcalls.entrySet().stream().forEach( entry -> {
                Stat childStat = parentStat.getChildren().get(entry.getKey());
                childStat.updateAvgHits(entry.getValue());
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
        s.lost = lost;
        s.fatalError = fatalError;
        s.startDate = startDate;
        s.endDate = endDate;

        map.forEach((k, v) -> s.map.put(k, v.deepCopy(withChildren)));

        return s;
    }

    @Transient
    public void merge(Stats inc, boolean withChildren){
        //too much hassle with merging statsLeft

        lost = lost + inc.lost;
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

    @Transient
    public void incrementLost() {
        lost++;
    }

    public Map<String, Stat> getMap() {
        return map;
    }

    public void setMap(Map<String, Stat> map) {
        this.map = map;
    }

    public long getLost() {
        return lost;
    }

    public void setLost(long lost) {
        this.lost = lost;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stats)) return false;

        Stats stats = (Stats) o;

        if (statsLeft != stats.statsLeft) return false;
        if (lost != stats.lost) return false;
        if (map != null ? !map.equals(stats.map) : stats.map != null) return false;
        if (fatalError != null ? !fatalError.equals(stats.fatalError) : stats.fatalError != null) return false;
        if (startDate != null ? !startDate.equals(stats.startDate) : stats.startDate != null) return false;
        return endDate != null ? endDate.equals(stats.endDate) : stats.endDate == null;

    }

    @Override
    public int hashCode() {
        int result = map != null ? map.hashCode() : 0;
        result = 31 * result + (int) (statsLeft ^ (statsLeft >>> 32));
        result = 31 * result + (int) (lost ^ (lost >>> 32));
        result = 31 * result + (fatalError != null ? fatalError.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        return result;
    }
}
