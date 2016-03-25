package org.endoscope.storage;

import org.endoscope.core.Stat;

import java.util.ArrayList;
import java.util.List;

public class StatDetails {
    private String id;
    private Stat merged;
    private List<StatHistory> histogram = new ArrayList<>();

    public StatDetails() {
        merged = new Stat();
    }

    public StatDetails(Stat merged) {
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
}
