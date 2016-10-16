package com.github.endoscope.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filters {
    public static Filters EMPTY = new Filters(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    
    List<String> instances;
    List<String> types;

    public Filters(){
        this.instances = new ArrayList<>();
        this.types = new ArrayList<>();
    }

    public Filters(List<String> instances, List<String> types){
        this.instances = instances;
        this.types = types;
    }

    public List<String> getInstances() {
        return instances;
    }

    public void setInstances(List<String> instances) {
        this.instances = instances;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }
}
