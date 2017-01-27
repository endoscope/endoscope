package com.github.endoscope.core;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.storage.StorageFactory;

import java.util.LinkedList;

public class Engine {
    private ThreadLocal<LinkedList<Context>> contextStack = new ThreadLocal<>();
    private Boolean enabled = null;
    private Storage storage = null;//may stay null if disabled or cannot setup it
    private CurrentStats currentStats;
    private CurrentStatsAsyncTasks currentStatsAsyncTasks;
    private int maxIdLength = Properties.getMaxIdLength();

    public Engine(){
        if( isEnabled()) {
            storage = new StorageFactory().safeCreate();//may return null
            currentStats = new CurrentStats();
            currentStatsAsyncTasks = new CurrentStatsAsyncTasks(currentStats, storage);
        }
    }

    public boolean isEnabled(){
        if( enabled == null ){
            enabled = Properties.isEnabled();
        }
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void checkEnabled(){
        if( !isEnabled() ){
            throw new IllegalStateException("feature not enabled");
        }
    }
    /**
     *
     * @param id required, might get cut if too long
     * @return true if it was first element pushed to call stack
     */
    public boolean push(String id){
        checkEnabled();

        id = prepareId(id);
        Context context = new Context(id, System.currentTimeMillis());

        LinkedList<Context> stack = contextStack.get();
        boolean first = false;
        if( stack == null ){
            first = true;
            stack = new LinkedList<>();
            contextStack.set(stack);
        }
        Context parent = stack.peek();
        if( parent != null ){
            parent.addChild(context);
        }
        stack.push(context);
        return first;
    }

    private String prepareId(String id){
        if( id == null ){
            return "<null>";
        }
        if( id.isEmpty() ){
            return "<empty>";
        }
        if( id.length() > maxIdLength ){
            return id.substring(0, maxIdLength);
        }
        return id;
    }

    public void pop(){
        checkEnabled();

        LinkedList<Context> stack = contextStack.get();
        if( stack.isEmpty() ){
            return;
        }
        Context context = stack.pop();
        context.setTime(System.currentTimeMillis() - context.getTime());

        if( stack.isEmpty() ){
            currentStats.add(context);
            currentStatsAsyncTasks.triggerAsyncTask();
        }
    }

    public void popAll(){
        checkEnabled();

        LinkedList<Context> stack = contextStack.get();
        Context context = null;
        while(!stack.isEmpty()){
            context = stack.pop();
            context.setTime(System.currentTimeMillis() - context.getTime());
        }
        if( context != null ){
            currentStats.add(context);
            currentStatsAsyncTasks.triggerAsyncTask();
        }
    }

    public CurrentStats getCurrentStats() {
        checkEnabled();
        return currentStats;
    }

    public Storage getStorage(){
        checkEnabled();
        return storage;
    }

    public CurrentStatsAsyncTasks getCurrentStatsAsyncTasks() {
        return currentStatsAsyncTasks;
    }
}
