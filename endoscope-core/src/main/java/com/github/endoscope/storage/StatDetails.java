package com.github.endoscope.storage;

import com.github.endoscope.core.Stat;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StatDetails {
    private String id;
    private Stat merged;
    private List<StatHistory> histogram = new ArrayList<>();

    public StatDetails() {
        merged = new Stat();
    }

    public StatDetails(String id, Stat merged) {
        this.id = id;
        this.merged = merged;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Stat getMerged() {
        return merged;
    }

    public void setMerged(Stat merged) {
        this.merged = merged;
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
        if( getMerged() == null ){
            setMerged(details.deepCopy(true));
        } else {
            getMerged().merge(details, true);
        }
        getHistogram().add( new StatHistory(details, startDate, endDate) );
    }
}
