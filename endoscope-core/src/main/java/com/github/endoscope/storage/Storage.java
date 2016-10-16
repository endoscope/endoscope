package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;

import java.util.Date;
import java.util.List;

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
     * @param topLevelOnly
     * @param from optional
     * @param to optional
     * @param instance optional
     * @param type optional
     * @return not null
     */
    Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type);
}
