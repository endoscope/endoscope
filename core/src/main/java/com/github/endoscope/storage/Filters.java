package com.github.endoscope.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filters {
    public static Filters EMPTY = new Filters(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    
    List<String> groups;
    List<String> types;

    public Filters(){
        this.groups = new ArrayList<>();
        this.types = new ArrayList<>();
    }

    public Filters(List<String> groups, List<String> types){
        this.groups = groups;
        this.types = types;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }
}
