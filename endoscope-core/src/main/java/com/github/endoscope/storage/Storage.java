package com.github.endoscope.storage;

import java.util.Date;
import java.util.List;

import com.github.endoscope.core.Stats;

public interface Storage {
    void setup(String initParam);

    /**
     * @param stats not null
     * @param instance optional
     * @param type optional
     * @return not null
     */
    String save(Stats stats, String instance, String type);

    /**
     * @param statsId optional - if empty then no need to remove anything
     * @param stats not null
     * @param instance optional
     * @param type optional
     * @return not null
     */
    String replace(String statsId, Stats stats, String instance, String type);
        
    /**
     * @param id not null
     * @return null if not exists
     */
    Stats load(String id);

    /**
     *
     * @param from optional
     * @param to optional
     * @param instance optional
     * @param type optional
     * @return not null but might be empty
     */
    List<String> find(Date from, Date to, String instance, String type);

    /**
     *
     * @param from optional
     * @param to optional
     * @param type optional
     * @return not null
     */
    Filters findFilters(Date from, Date to, String type);

    //performance methods

    /**
     * @param detailsId not null
     * @param statsIds not null
     * @return not null
     */
    StatDetails loadDetails(String detailsId, List<String> statsIds);

    /**
     * @param detailsId not null
     * @param from optional
     * @param to optional
     * @param instance optional
     * @param type optional
     * @return not null
     */
    StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type);

    /**
     * @param detailsId not null
     * @param from optional
     * @param to optional
     * @param instance optional
     * @param type optional
     * @param lastGroupId optional
     *                    Load histogram part starting at next group after this one.
     *                    You can get this value from result. If present it means that only part of histogram was
     *                    returned and you need to to call service again to get next parts until this value is null.
     *
     *                    Implementation may ignore it but in such case it will not return it either.
     * @return not null
     */
    Histogram loadHistogram(String detailsId, Date from, Date to, String instance, String type, String lastGroupId);

    /**
     * @param topLevelOnly
     * @param from optional
     * @param to optional
     * @param instance optional
     * @param type optional
     * @return not null
     */
    Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type);

    /**
     * Removes some of old and no longer used stats.
     * It's optional operation. Implementation might do nothing.
     *
     * @param daysToKeep cleanup stats older than this value
     * @param type
     */
    void cleanup(int daysToKeep, String type);
}
