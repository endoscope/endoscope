package org.endoscope.storage;

import org.endoscope.core.Stats;

/**
 * Implementation class should have public constructor that accepts single String parameter.
 */
public abstract class StatsStorage {
    public StatsStorage(String initParam){}
    /**
     * Save stats.
     * @param stats
     * @return stats identifier
     */
    public abstract String save(Stats stats);
}
