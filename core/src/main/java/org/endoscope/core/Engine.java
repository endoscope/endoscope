package org.endoscope.core;

import org.endoscope.properties.Properties;
import org.endoscope.storage.StatsCyclicWriter;
import org.endoscope.storage.StatsStorage;
import org.endoscope.storage.StatsStorageFactory;

import java.util.LinkedList;

public class Engine {
    private ThreadLocal<LinkedList<Context>> contextStack = new ThreadLocal<>();
    private Boolean enabled = null;
    private StatsStorage statsStorage = null;//may stay null if disabled or cannot setup it
    private StatsCyclicWriter statsCyclicWriter;
    private StatsProcessor statsProcessor;
    private int maxIdLength = Properties.getMaxIdLength();

    public Engine(){
        statsStorage = new StatsStorageFactory().safeCreate();//may return null
        statsCyclicWriter = new StatsCyclicWriter(statsStorage);
        statsProcessor = new StatsProcessor(statsCyclicWriter);
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

    /**
     *
     * @param id required, might get cut if too long
     * @return true if it was first element pushed to call stack
     */
    public boolean push(String id){
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
        LinkedList<Context> stack = contextStack.get();
        if( stack.isEmpty() ){
            return;
        }
        Context context = stack.pop();
        context.setTime(System.currentTimeMillis() - context.getTime());

        if( stack.isEmpty() ){
            statsProcessor.store(context);
        }
    }

    public void popAll(){
        LinkedList<Context> stack = contextStack.get();
        Context context = null;
        while(!stack.isEmpty()){
            context = stack.pop();
            context.setTime(System.currentTimeMillis() - context.getTime());
        }
        if( context != null ){
            statsProcessor.store(context);
        }
    }

    public StatsProcessor getStatsProcessor() {
        return statsProcessor;
    }

    public StatsStorage getStatsStorage(){ return statsStorage; }
}
