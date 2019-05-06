package com.github.endoscope.core;

import java.util.LinkedList;
import java.util.function.Supplier;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.storage.StorageFactory;
import org.apache.commons.lang3.StringUtils;

public class Engine {
    private ThreadLocal<LinkedList<Context>> contextStack = new ThreadLocal<>();
    private Boolean enabled = null;
    private Storage storage = null;//may stay null if disabled or cannot setup it
    private CurrentStats currentStats;
    private AsyncTasksFactory currentStatsAsyncTasks;
    private int maxIdLength = Properties.getMaxIdLength();

    public Engine() {
        if (isEnabled()) {
            storage = new StorageFactory().safeCreate();//may return null
            currentStats = new CurrentStats();
            currentStatsAsyncTasks = new CurrentStatsAsyncTasks(currentStats, storage);
        }
    }

    protected Engine(boolean enabled, Storage storage, AsyncTasksFactory tasksFactory) {
        this.enabled = enabled;
        this.storage = storage;
        currentStats = new CurrentStats();
        currentStatsAsyncTasks = tasksFactory;
    }

    public boolean isEnabled() {
        if (enabled == null) {
            enabled = Properties.isEnabled();
        }
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void checkEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("feature not enabled");
        }
    }

    /**
     * @param id required, might get cut if too long
     * @return true if it was first element pushed to call stack
     */
    protected boolean push(String id) {
        checkEnabled();

        id = prepareId(id);
        Context context = new Context(id, System.currentTimeMillis());

        LinkedList<Context> stack = contextStack.get();
        boolean first = false;
        if (stack == null) {
            first = true;
            stack = new LinkedList<>();
            contextStack.set(stack);
        }
        Context parent = stack.peek();
        if (parent != null) {
            parent.addChild(context);
        }
        stack.push(context);
        return first;
    }

    private String prepareId(String id) {
        id = StringUtils.trimToNull(id);
        if (id == null) {
            return "<blank>";
        }
        if (id.length() > maxIdLength) {
            return id.substring(0, maxIdLength);
        }
        return id;
    }

    protected void pop(boolean completedWithException) {
        checkEnabled();

        LinkedList<Context> stack = contextStack.get();
        if (stack.isEmpty()) {
            return;
        }
        Context context = stack.pop();
        context.setTime(System.currentTimeMillis() - context.getTime());
        context.setErr(completedWithException);

        if (stack.isEmpty()) {
            currentStats.add(context);
            currentStatsAsyncTasks.triggerAsyncTask();
        }
    }

    /*
     * It should always pop last element - but in case we made error we need to make sure we clean the stack.
     */
    protected void popAll(boolean completedWithException) {
        checkEnabled();

        LinkedList<Context> stack = contextStack.get();
        Context context = null;
        while (!stack.isEmpty()) {
            context = stack.pop();
            context.setTime(System.currentTimeMillis() - context.getTime());
            context.setErr(completedWithException);
        }
        if (context != null) {
            currentStats.add(context);
            currentStatsAsyncTasks.triggerAsyncTask();
        }
    }

    public CurrentStats getCurrentStats() {
        checkEnabled();
        return currentStats;
    }

    public Storage getStorage() {
        checkEnabled();
        return storage;
    }

    public AsyncTasksFactory getCurrentStatsAsyncTasks() {
        return currentStatsAsyncTasks;
    }

    /**
     * Groups operations under one identifier (next element on monitoring stack).
     *
     * @param id       operation identifier
     * @param runnable result runnable
     */
    public void monitor(String id, Runnable runnable) {
        monitor(id, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Groups operations under one identifier (next element on monitoring stack).
     *
     * @param id       operation identifier
     * @param supplier result supplier
     * @param <T>
     * @return supplier result
     */
    public <T> T monitor(String id, Supplier<T> supplier) {
        if (!isEnabled()) {
            return supplier.get();
        }

        boolean first = false;
        boolean completedWithException = true;
        try {
            first = push(id);
            T value = supplier.get();
            completedWithException = false;
            return value;
        } finally {
            if (first) {
                popAll(completedWithException);
            } else {
                pop(completedWithException);
            }
        }
    }

    /**
     * Similar to #monitor(String, Supplier) but declared with thrown Exception.
     *
     * @param id       operation identifier
     * @param supplier result supplier that may throw Exception
     * @param <T>
     * @return supplier result
     */
    public <T> T monitorEx(String id, ExceptionalSupplier<T> supplier) throws Exception {
        if (!isEnabled()) {
            return supplier.get();
        }

        boolean first = false;
        boolean completedWithException = true;
        try {
            first = push(id);
            T value = supplier.get();
            completedWithException = false;
            return value;
        } finally {
            if (first) {
                popAll(completedWithException);
            } else {
                pop(completedWithException);
            }
        }
    }
}
