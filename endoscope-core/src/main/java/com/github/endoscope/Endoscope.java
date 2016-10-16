package com.github.endoscope;

import com.github.endoscope.core.Engine;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Storage;

import java.util.function.Function;

/**
 * Easy to use static facade.
 */
public class Endoscope {
    private static Engine ENGINE = new Engine();

    public static boolean isEnabled(){
        return ENGINE.isEnabled();
    }

    /**
     *
     * @param id required, might get cut if too long
     * @return true if it was first element pushed to call stack
     */
    public static boolean push(String id){
        return ENGINE.push(id);
    }

    public static void pop(){
        ENGINE.pop();
    }

    /**
     * Use it to finalize collecting data for current thread.
     * When Endoscope is correctly used in try/finally it just pops latest element.
     *
     * It may however prevent memory leak when Endoscope stack is not cleaned up correctly.
     * For example when pop is not put in finally and exception occurs.
     */
    public static void popAll(){
        ENGINE.popAll();
    }

    /**
     * This method blocks stats updating thread from storing new data.
     * Please do your job as quickly as possible otherwise internal queue will reach limit and you'll loose some data.
     * Do not expose objects outside - deep copy such object in order to keep it thread safe.
     * @param function
     */
    public static <T> T processStats(Function<Stats, T> function){
        return ENGINE.getStatsProcessor().process(function);
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
        ENGINE.getStatsProcessor().stopStatsProcessorThread();
    }

    /**
     * Clears current stats.
     */
    public static void resetStats(){
        ENGINE.getStatsProcessor().resetStats();
    }
}
