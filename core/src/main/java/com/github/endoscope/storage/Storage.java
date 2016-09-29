package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;

import java.util.Date;
import java.util.List;

public interface Storage {
    void setup(String initParam);

    String save(Stats stats, String instance, String type);
    Stats load(String id);
    List<String> find(Date from, Date to, String instance, String type);
    Filters findFilters(Date from, Date to, String type);

    //performance methods
    StatDetails loadDetails(String detailsId, List<String> stats);
    StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type);
    Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type);
}
