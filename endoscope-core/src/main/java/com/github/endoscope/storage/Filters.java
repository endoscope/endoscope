package com.github.endoscope.storage;

import java.util.ArrayList;
import java.util.List;

public class Filters {
    List<String> instances;
    List<String> types;
    String info;

    public Filters(){
        this(new ArrayList<>(), new ArrayList<>());
    }

    public Filters(List<String> instances, List<String> types){
        this(instances, types, null);
    }

    public Filters(List<String> instances, List<String> types, String info){
        this.instances = instances;
        this.types = types;
        this.info = info;
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
