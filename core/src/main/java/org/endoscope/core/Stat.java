package org.endoscope.core;

import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;

@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "hits", "max", "min", "avg", "ah10", "children" })
public class Stat {
    private long hits = 0;
    private long max = -1;//-1 means it's not set
    private long min = 0;
    private double avg = 0;

    /*
        Average hits per parent. For example:
        1) parentMethod calls childMethod 2 times
        2) parentMethod calls childMethod 4 times

        In such case we should get:
            parentCount==2
            avgParent==3.0

        Notice that we increment parentCount by one - thats the difference from hits
        and allows us to calulate average number of hits
     */
    private long parentCount = 0;
    double avgParent = 0;

    private Map<String, Stat> children;

    public Stat(){}

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getAvg() {
        return Math.round(avg);
    }

    public void setAvg(long avg) {
        this.avg = avg;
    }

    /**
     * Average hits per parent x10 (1 digit of precision)
     * Short name for JSON - no @JsonProperty in this module
     * @return
     */
    public long getAh10() {
        return Math.round(avgParent*10.0f);
    }

    public void setAh10(long ah10) {
        avgParent = ((float)ah10)/10f;
    }

    public Map<String, Stat> getChildren() {
        return children;
    }

    public void setChildren(Map<String, Stat> children) {
        this.children = children;
    }

    @Transient
    public long getParentCount() {
        return parentCount;
    }

    @Transient
    public void setParentCount(long parentCount) {
        this.parentCount = parentCount;
    }

    @Transient
    public double getAvgParent() {
        return avgParent;
    }

    @Transient
    public void setAvgParent(double avgParent) {
        this.avgParent = avgParent;
    }

    public void ensureChildrenMap(){
        if(children == null){
            children = new HashMap<>();
        }
    }

    @Transient
    public Stat getChild(String id){
        ensureChildrenMap();
        return children.get(id);
    }

    @Transient
    public Stat createChild(String id){
        ensureChildrenMap();
        Stat child = new Stat();
        children.put(id, child);
        return child;
    }

    public void update(long time){
        if( time < 0 ) return;
        if( max < 0 ){
            avg = max = min = time;
        } else {
            max = Math.max(max, time);
            min = Math.min(min, time);
            avg = (avg* hits + time)/(hits +1);
        }
        hits++;
    }

    public void updateAvgHits(long hitsPerParent) {
        avgParent = (avgParent * parentCount + hitsPerParent)/(parentCount+1);
        parentCount++;
    }

    @Transient
    public void merge(Stat inc){
        merge(inc, true);
    }

    @Transient
    public void merge(Stat inc, boolean withChildren){
        max = Math.max(max, inc.max);
        min = Math.min(min, inc.min);
        if( hits + inc.hits > 0 ){
            avg = (avg*hits + inc.avg*inc.hits)/(hits + inc.hits);
            hits += inc.hits;
        }

        if( parentCount + inc.parentCount > 0 ){
            avgParent = (avgParent * parentCount + inc.avgParent * inc.parentCount)/(parentCount + inc.parentCount);
            parentCount += inc.parentCount;
        }

        if( withChildren ){
            mergeChildren(inc);
        } else {
            //we want to mark that there are children
            if( inc.getChildren() != null ){
                ensureChildrenMap();
            }
        }
    }

    @Transient
    private void mergeChildren(Stat s2){
        if( s2.getChildren() == null ){
            return;
        }
        ensureChildrenMap();

        s2.children.forEach((k2, v2) -> {
            Stat v1 = children.get(k2);
            if( v1 == null ){
                children.put(k2, v2);
            } else {
                v1.merge(v2);
            }
        });
    }

    @Transient
    public Stat deepCopy(){
        return deepCopy(true);
    }

    @Transient
    public Stat deepCopy(boolean withChildren){
        Stat s = new Stat();
        s.merge(this, withChildren);
        s.setMin(min);
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stat)) return false;

        Stat stat = (Stat) o;

        if (hits != stat.hits) return false;
        if (max != stat.max) return false;
        if (min != stat.min) return false;
        if ( compareDoubleLowPrecision(stat.avg, avg) != 0) return false;
        if (parentCount != stat.parentCount) return false;
        if ( compareDoubleLowPrecision(stat.avgParent, avgParent) != 0) return false;
        return children != null ? children.equals(stat.children) : stat.children == null;
    }

    public static int compareDoubleLowPrecision(double d1, double d2){
        long l1 = Math.round(d1 * 1000);
        long l2 = Math.round(d2 * 1000);
        return Long.compare(l1, l2);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (hits ^ (hits >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + (int) (min ^ (min >>> 32));
        temp = Double.doubleToLongBits(avg);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (parentCount ^ (parentCount >>> 32));
        temp = Double.doubleToLongBits(avgParent);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Stat{" +
                "hits=" + hits +
                ", max=" + max +
                ", min=" + min +
                ", avg=" + avg +
                ", parentCount=" + parentCount +
                ", avgParent=" + avgParent +
                ", children=" + children +
                '}';
    }
}
