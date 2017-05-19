package com.github.endoscope.storage;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.endoscope.core.Stat;

public class Histogram {
    private String id;
    private List<StatHistory> histogram = new ArrayList<>();
    private String info;
    private String lastGroupId;
    private Date startDate;
    private Date endDate;

    public Histogram(){}
    public Histogram(String id){ this.id = id;}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<StatHistory> getHistogram() {
        return histogram;
    }

    public void setHistogram(List<StatHistory> histogram) {
        this.histogram = histogram;
    }

    @Transient
    public void add(Stat details, Date startDate, Date endDate){
        if( details == null ){
            return;
        }
        getHistogram().add( new StatHistory(details, startDate, endDate) );
    }

    @Transient
    public boolean isLastHistogramPart(){
        return lastGroupId == null;
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

    /**
     * When set then it means that histogram is just a part that ends at given group.
     * Call again to get next parts until this value is null;
     * @return
     */
    public String getLastGroupId() {
        return lastGroupId;
    }

    public void setLastGroupId(String lastGroupId) {
        this.lastGroupId = lastGroupId;
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
