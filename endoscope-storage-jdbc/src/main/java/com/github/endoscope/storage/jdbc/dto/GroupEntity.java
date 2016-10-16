package com.github.endoscope.storage.jdbc.dto;

import com.github.endoscope.core.Stats;

import java.beans.Transient;

public class GroupEntity extends Stats {
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
