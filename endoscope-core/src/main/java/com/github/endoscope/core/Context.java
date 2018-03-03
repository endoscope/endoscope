package com.github.endoscope.core;

import java.util.LinkedList;
import java.util.List;

/**
 * This class represents method invocation tree.
 * You can "add" such tree to Stats in order to update global statistics.
 */
public class Context {
    private String id;
    private long time;
    private boolean err;
    private List<Context> children;

    public Context() {
    }

    public Context(String id, long time) {
        this.id = id;
        this.time = time;
    }

    public void addChild(Context child){
        if( children == null ){
            children = new LinkedList<>();
        }
        children.add(child);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<Context> getChildren() {
        return children;
    }

    public void setChildren(List<Context> children) {
        this.children = children;
    }

    public boolean isErr() {
        return err;
    }

    public void setErr(boolean err) {
        this.err = err;
    }
}
