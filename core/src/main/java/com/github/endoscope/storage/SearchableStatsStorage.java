package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;

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
     * @param to appGroup
     * @param to appType
     * @return
     */
    Stats topLevel(Date from, Date to, String appGroup, String appType);

    /**
     * Complete root stats.
     * @param id required
     * @param from required
     * @param to required
     * @param to appGroup
     * @param to appType
     * @return
     */
    StatDetails stat(String id, Date from, Date to, String appGroup, String appType);

    /**
     * Optional - groups and types in given time period.
     * @param from
     * @param to
     * @return not null filters - lists might be empty
     */
    Filters filters(Date from, Date to);
}
