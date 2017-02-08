package com.github.endoscope.storage;

import com.github.endoscope.core.Stat;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Histogram {
    private String id;
    private List<StatHistory> histogram = new ArrayList<>();
    private String info;

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
}
