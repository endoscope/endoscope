package org.endoscope.storage.gzip;

import org.endoscope.core.Stat;
import org.endoscope.core.Stats;
import org.endoscope.storage.SearchableStatsStorage;
import org.endoscope.storage.StatDetails;
import org.endoscope.storage.StatHistory;
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
    public Stats topLevel(Date from, Date to) {
        log.info("Searching for top level stats from {} to {}", getDateFormat().format(from), getDateFormat().format(to));
        Stats merged = new Stats();
        listParts().stream()
                .peek( statsInfo -> log.info("Checking {}", statsInfo.getName()))
                .filter(statsInfo -> statsInfo.inRange(from, to))
                .peek( statsInfo -> log.info("Matches: {}", statsInfo.getName()))
                .map( statsInfo -> load(statsInfo.getName()))
                .forEach(stats -> merged.merge(stats, false));
        return merged;
    }

    @Override
    public StatDetails stat(String id, Date from, Date to) {
        log.info("Searching for stat {} from {} to {}", id, getDateFormat().format(from), getDateFormat().format(to));
        StatDetails result = new StatDetails(null);
        result.setId(id);

        listParts().stream()
                .peek( fileInfo -> log.info("Checking {}", fileInfo.getName()))
                .filter(fileInfo -> fileInfo.inRange(from, to))
                .peek( fileInfo -> log.info("Matches: {}", fileInfo.getName()))
                .forEach( fileInfo -> {
                    Stats stats = load(fileInfo.getName());
                    Stat details = stats.getMap().get(id);
                    if( details != null ){
                        if( result.getMerged() == null ){
                            result.setMerged(details.deepCopy(true));
                        } else {
                            result.getMerged().merge(details, true);
                        }
                        //TODO merge to no more than 100 points
                        result.getHistogram().add(
                                new StatHistory(
                                    details,
                                    stats.getStartDate(),
                                    stats.getEndDate()
                                ));

                    }
                });
        if( result.getMerged() == null ){
            result.setMerged(new Stat());
        }
        return result;
    }
}
