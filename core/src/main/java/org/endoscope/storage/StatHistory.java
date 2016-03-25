package org.endoscope.storage;

import org.endoscope.core.Stat;

import java.util.Date;

public class StatHistory {
    private long hits = 0;
    private long max = -1;//-1 means it's not set
    private long min = 0;
    private long avg = 0;
    private Date startDate;
    private Date endDate;

    public StatHistory(Stat stat, Date startDate, Date endDate) {
        hits = stat.getHits();
        max = stat.getMax();
        min = stat.getMin();
        avg = stat.getAvg();

        this.startDate = startDate;
        this.endDate = endDate;
    }

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
        return avg;
    }

    public void setAvg(long avg) {
        this.avg = avg;
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
}
