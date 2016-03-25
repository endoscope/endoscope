package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;

public class StatInfo {
    private Stat stat;
    private String name;
    private String id;

    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
