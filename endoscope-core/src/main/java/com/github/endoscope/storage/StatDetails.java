package com.github.endoscope.storage;

import com.github.endoscope.core.Stat;

import java.beans.Transient;

public class StatDetails {
    private String id;
    private Stat merged;
    private String info;

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

    @Transient
    public void add(Stat details){
        if( details == null ){
            return;
        }
        if( getMerged() == null ){
            setMerged(details.deepCopy(true));
        } else {
            getMerged().merge(details, true);
        }
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
