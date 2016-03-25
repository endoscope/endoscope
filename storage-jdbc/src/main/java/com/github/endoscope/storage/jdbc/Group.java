package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stats;

import java.beans.Transient;

public class Group extends Stats {
    private String id;

    @Transient
    public String getId() {
        return id;
    }

    @Transient
    public void setId(String id) {
        this.id = id;
    }
}
