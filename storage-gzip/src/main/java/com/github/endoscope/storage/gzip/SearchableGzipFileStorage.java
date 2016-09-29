package com.github.endoscope.storage.gzip;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.SearchableStatsStorage;
import com.github.endoscope.storage.StatDetails;
import org.slf4j.Logger;

import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This storage is for demo purposes as it's not efficient.
 * Notice that it loads complete stats in order to extract just part of it.
 */
public class SearchableGzipFileStorage extends GzipFileStorage implements SearchableStatsStorage {
    private static final Logger log = getLogger(SearchableGzipFileStorage.class);

    public SearchableGzipFileStorage(String dir){
        super(dir);
    }

    @Override
    public Stats topLevel(Date from, Date to, String appInstance, String appType) {
        log.debug("Searching for top level stats from {} to {}", getDateFormat().format(from), getDateFormat().format(to));
        Stats merged = new Stats();
        listParts().stream()
                .peek( statsInfo -> log.debug("Checking {}", statsInfo.build()))
                .filter(statsInfo -> statsInfo.match(from, to, null, null))
                .peek( statsInfo -> log.debug("Matches: {}", statsInfo.build()))
                .map( statsInfo -> load(statsInfo.build()))
                .forEach(stats -> merged.merge(stats, false));
        return merged;
    }

    @Override
    public StatDetails stat(String id, Date from, Date to, String appInstance, String appType) {
        log.debug("Searching for stat {} from {} to {}", id, getDateFormat().format(from), getDateFormat().format(to));
        StatDetails result = new StatDetails(id, null);

        listParts().stream()
                .peek( fileInfo -> log.debug("Checking {}", fileInfo.build()))
                .filter(fileInfo -> fileInfo.match(from, to, null, null))
                .peek( fileInfo -> log.debug("Matches: {}", fileInfo.build()))
                .forEach( fileInfo -> {
                    Stats stats = load(fileInfo.build());
                    Stat details = stats.getMap().get(id);
                    result.add(details, stats.getStartDate(), stats.getEndDate());
                });
        if( result.getMerged() == null ){
            result.setMerged(Stat.emptyStat());
        }
        return result;
    }

    @Override
    public Filters filters(Date from, Date to) {
        return new Filters();
    }
}
