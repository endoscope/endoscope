package com.github.endoscope.storage.jdbc.dto;

import com.github.endoscope.core.Stat;

import java.util.Objects;

public class StatEntity {
    private String id;
    private String groupId;
    private String parentId;
    private String name;

    private Stat stat = new Stat();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatEntity)) return false;
        StatEntity that = (StatEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(parentId, that.parentId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(stat, that.stat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupId, parentId, name, stat);
    }
}
