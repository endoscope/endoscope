package com.github.endoscope;

import java.util.function.Function;
import java.util.function.Supplier;

import com.github.endoscope.core.Engine;
import com.github.endoscope.core.ExceptionalSupplier;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Storage;

public class Endoscope {
    private static Engine ENGINE = new Engine();

    public static boolean isEnabled(){
        return ENGINE.isEnabled();
    }

    /**
     * Groups operations under one identifier (next element on monitoring stack).
     *
     * @param id operation identifier
     * @param supplier result supplier
     * @param <T>
     * @return supplier result
     */
    public static <T> T monitor(String id, Supplier<T> supplier) {
        return ENGINE.monitor(id, supplier);
    }

    /**
     * Groups operations under one identifier (next element on monitoring stack).
     *
     * @param id operation identifier
     * @param runnable result runnable
     */
    public static void monitor(String id, Runnable runnable) {
        ENGINE.monitor(id, runnable);
    }

    /**
     * Similar to #monitor(String, Supplier) but declared with thrown Exception.
     *
     * @param id operation identifier
     * @param supplier result supplier that may throw Exception
     * @param <T>
     * @return supplier result
     */
    public static <T> T monitorEx(String id, ExceptionalSupplier<T> supplier) throws Exception {
        return ENGINE.monitorEx(id, supplier);
    }

    /**
     * This method blocks stats updating thread from storing new data.
     * Please do your job as quickly as possible otherwise internal queue will reach limit and you'll loose some data.
     * Do not expose objects outside - deep copy such object in order to keep it thread safe.
     * @param function
     */
    public static <T> T processStats(Function<Stats, T> function){
        return ENGINE.getCurrentStats().lockReadStats(function);
    }

    /**
     * Returns deep copy (thread safe) of whole stats. It might be time consuming do make such copy in case of huge stats.
     * If you need just part of stats please consider using {@link #processStats(Function)}.
     * @return
     */
    public static Stats getCurrentStats(){
        Stats[] result = new Stats[]{null};
        processStats(stats -> result[0] = stats.deepCopy());
        return result[0];
    }

    /**
     * Access to stored stats.
     * @return null if not supported
     */
    public static Storage getStatsStorage(){ return ENGINE.getStorage(); }

    public static void stopStatsProcessorThread(){
        ENGINE.getCurrentStatsAsyncTasks().stopStatsProcessorThread();
    }

    /**
     * Clears current stats.
     */
    public static void resetStats(){
        ENGINE.getCurrentStats().resetStats();
    }

    public static long getQueueSize(){
        return ENGINE.getCurrentStats().getQueueSize();
    }
}
