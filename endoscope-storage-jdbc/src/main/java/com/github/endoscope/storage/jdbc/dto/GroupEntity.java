package com.github.endoscope.storage.jdbc.dto;

import com.github.endoscope.core.Stats;

import java.beans.Transient;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupEntity)) return false;
        if (!super.equals(o)) return false;
        GroupEntity that = (GroupEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
