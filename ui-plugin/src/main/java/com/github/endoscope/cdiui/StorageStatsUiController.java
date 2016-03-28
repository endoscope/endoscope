package com.github.endoscope.cdiui;

import com.github.endoscope.Endoscope;
import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.StatHistory;
import com.github.endoscope.util.JsonUtil;
import com.github.endoscope.storage.SearchableStatsStorage;
import com.github.endoscope.storage.StatDetails;
import org.slf4j.Logger;

import javax.ws.rs.*;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/endoscope/storage")
public class StorageStatsUiController extends StaticResourceController{
    private static final Logger log = getLogger(StorageStatsUiController.class);

    private JsonUtil jsonUtil = new JsonUtil();

    @GET
    @Path("ui/data/top")
    @Produces("application/json")
    public String top(@QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("past") String past) {
        Stats stats = topLevel(toLong(from), toLong(to), toLong(past));
        return jsonUtil.toJson(stats.getMap());
    }

    @GET
    @Path("ui/data/details/{id}")
    @Produces("application/json")
    public String details(@PathParam("id") String id, @QueryParam("from") String from, @QueryParam("to") String to,
                      @QueryParam("past") String past){
        StatDetails child = stat(id, toLong(from), toLong(to), toLong(past));
        return jsonUtil.toJson(child);
    }

    private Long toLong(String value){
        if( value == null || value.trim().length() == 0 ){
            return null;
        }
        return Long.valueOf(value);
    }

    private boolean isRange(Long from, Long to){
        return from != null && to != null;
    }

    private Stats topLevel(Long from, Long to, Long past){
        boolean includeCurrent = false;
        if( past != null ){
            to = System.currentTimeMillis();
            from = to - past;
            includeCurrent = true;
        }
        if( !isRange(from, to) ){
            return topLevelInMemory();
        }
        return topLevelForRange(from, to, includeCurrent);
    }

    private StatDetails stat(String id, Long from, Long to, Long past){
        boolean includeCurrent = false;
        if( past != null ){
            to = System.currentTimeMillis();
            from = to - past;
            includeCurrent = true;
        }
        if( !isRange(from, to) ){
            StatDetails result = detailsInMemory(id);
            return (result != null) ? result : new StatDetails(id, Stat.EMPTY_STAT);
        }
        return detailsForRange(id, from, to, includeCurrent);
    }

    private Stats topLevelForRange(Long from, Long to, boolean includeCurrent) {
        Stats result = getSearchableStatsStorage().topLevel(new Date(from), new Date(to));
        if( includeCurrent ){
            Stats current = topLevelInMemory();
            result.merge(current, false);

            log.info("current start time: {}", current.getStartDate());
            log.info("current end time: {}", current.getEndDate());
        }
        return result;
    }

    private StatDetails detailsForRange(String id, Long from, Long to, boolean includeCurrent) {
        StatDetails result = getSearchableStatsStorage().stat(id, new Date(from), new Date(to));
        if( includeCurrent ){
            StatDetails current = detailsInMemory(id);
            if( current != null ){
                //we don't want to merge not set stats as it would reset min value to 0
                result.getMerged().merge(current.getMerged(), true);
                result.getHistogram().addAll(current.getHistogram());
            }
        }
        return result;
    }

    private Stats topLevelInMemory() {
        return Endoscope.processStats(stats -> stats.deepCopy(false) );
    }

    private StatDetails detailsInMemory(String id) {
        return Endoscope.processStats(stats -> {
            Stat s = stats.getMap().get(id);
            if( s == null ){//might happen when stats get saved and/or reset in the mean time
                return null;
            }
            StatDetails result = new StatDetails();
            s = s.deepCopy();
            result.setMerged(s);
            result.getHistogram().add(
                new StatHistory(s, stats.getStartDate(), new Date())
            );
            return result;
        });
    }

    private SearchableStatsStorage getSearchableStatsStorage() {
        SearchableStatsStorage storage;
        try{
            storage = (SearchableStatsStorage) Endoscope.getStatsStorage();
        }catch(ClassCastException e){
            throw new RuntimeException("Range search is not supported");
        }
        return storage;
    }
}
