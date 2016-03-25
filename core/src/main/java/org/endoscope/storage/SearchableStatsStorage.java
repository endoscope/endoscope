package org.endoscope.storage;

import org.endoscope.core.Stats;

import java.util.Date;

/**
 * Implementation class should have public constructor that accepts single String parameter.
 */
public interface SearchableStatsStorage {
    /**
     * If root stats have children than they should be set to map object - might be empty.
     * Otherwise children property shpuld be null.
     *
     * For simplicity it might include children of root stats - they should be ignored during serialization.
     * @param from required
     * @param to required
     * @return
     */
    Stats topLevel(Date from, Date to);

    /**
     * Complete root stats.
     * @param id required
     * @param from required
     * @param to required
     * @return
     */
    StatDetails stat(String id, Date from, Date to);
}
