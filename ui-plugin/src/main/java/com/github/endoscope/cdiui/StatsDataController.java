package com.github.endoscope.cdiui;

import com.github.endoscope.Endoscope;
import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.SearchableStatsStorage;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.StatHistory;
import com.github.endoscope.util.JsonUtil;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/endoscope")
public class StatsDataController {
    private static final Logger log = getLogger(StatsDataController.class);

    private JsonUtil jsonUtil = new JsonUtil();

    protected Response noCacheResponse( Object entity ) {
        CacheControl cc = new CacheControl();
        cc.setNoCache( true );
        cc.setMaxAge( -1 );
        cc.setMustRevalidate( true );

        return Response.ok( entity ).cacheControl( cc ).build();
    }

    @GET
    @Path("/data/top")
    @Produces("application/json")
    public Response top(@QueryParam("from") String fromS,
                        @QueryParam("to") String toS,
                        @QueryParam("past") String pastS,
                        @QueryParam("group") String group,
                        @QueryParam("type") String type,
                        @QueryParam("reset") boolean reset) {
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);

        if( reset ){
            log.info("Resetting current stats");
            Endoscope.resetStats();
        }

        Stats stats = topLevelForRange(new Range(from, to, past, group, type));
        return noCacheResponse(jsonUtil.toJson(stats.getMap()));
    }

    @GET
    @Path("/data/top/daily")
    @Produces("application/json")
    public Response topDaily(@QueryParam("from") String fromS,
                        @QueryParam("to") String toS,
                        @QueryParam("past") String pastS,
                        @QueryParam("group") String group,
                        @QueryParam("type") String type,
                        @QueryParam("reset") boolean reset) {
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);

        if( reset ){
            log.info("Resetting current stats");
            Endoscope.resetStats();
        }

        //TODO "#daily#" is a temporary solution for debug purposes - before we permanently switch to this version
        Stats stats = topLevelForRange(new Range(from, to, past, "#daily#", type));
        return noCacheResponse(jsonUtil.toJson(stats.getMap()));
    }

    private Long toLong(String value){
        if( value == null || value.trim().length() == 0 ){
            return null;
        }
        return Long.valueOf(value);
    }

    @GET
    @Path("/data/details")
    @Produces("application/json")
    public Response details(@QueryParam("id") String id,
                            @QueryParam("from") String fromS,
                            @QueryParam("to") String toS,
                            @QueryParam("past") String pastS,
                            @QueryParam("group") String group,
                            @QueryParam("type") String type){
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);
        StatDetails child = detailsForRange(id, new Range(from, to, past, group, type));
        return noCacheResponse(jsonUtil.toJson(child));
    }

    private static class Range {
        public Range(Long from, Long to, Long past, String group, String type){
            this.group = group;
            this.type = type;
            if( past != null ){
                if( past > 0 ){
                    toDate = new Date();
                    fromDate = new Date(toDate.getTime() - past);
                }
            } else {
                if( from != null ){
                    fromDate = new Date(from);

                    //to requires from
                    if( to != null ){
                        toDate = new Date(to);
                        includeCurrent = false;
                    } else {
                        toDate = new Date();
                    }
                }
            }
        }

        Date fromDate = null;
        Date toDate = null;
        String group;
        String type;
        boolean includeCurrent = true;
    }

    private Stats topLevelForRange(Range range) {
        Stats result;
        if( canSearch(range) ){
            result = getSearchableStatsStorage().topLevel(range.fromDate, range.toDate, range.group, range.type);
            if( range.includeCurrent ){
                Stats current = topLevelInMemory();
                result.merge(current, false);
            }
        } else{
            result = topLevelInMemory();
        }
        return result;
    }

    private StatDetails detailsForRange(String id, Range range) {
        StatDetails result;
        if( canSearch(range) ){
            result = getSearchableStatsStorage().stat(id, range.fromDate, range.toDate, range.group, range.type);
            if( range.includeCurrent ){
                StatDetails current = detailsInMemory(id);
                if( current != null ){
                    //we don't want to merge not set stats as it would reset min value to 0
                    result.getMerged().merge(current.getMerged(), true);
                    result.getHistogram().addAll(current.getHistogram());
                }
            }
        } else {
            result = detailsInMemory(id);
        }
        return (result != null) ? result : new StatDetails(id, Stat.emptyStat());
    }

    private boolean canSearch(Range range){
        return range.fromDate != null && range.toDate != null
                && Endoscope.getStatsStorage() != null;
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
            if( storage == null ){
                throw new RuntimeException("Storage not supported");
            }
        }catch(ClassCastException e){
            throw new RuntimeException("Range search is not supported");
        }
        return storage;
    }


    @GET
    @Path("/data/filters")
    @Produces("application/json")
    public Response filters(@QueryParam("from") String fromS,
                            @QueryParam("to") String toS,
                            @QueryParam("past") String pastS){
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);
        Range range = new Range(from, to, past, null, null);
        Filters filters = null;
        if( canSearch(range) ){
            filters = getSearchableStatsStorage().filters(range.fromDate, range.toDate);
        }
        if( filters == null){
            filters = Filters.EMPTY;
        }
        if( !filters.getGroups().contains(Properties.getAppInstance()) ){
            filters.getGroups().add(Properties.getAppInstance());
        }
        if( !filters.getTypes().contains(Properties.getAppType()) ){
            filters.getTypes().add(Properties.getAppType());
        }
        Collections.sort(filters.getGroups());
        Collections.sort(filters.getTypes());
        return noCacheResponse(jsonUtil.toJson(filters));
    }

    private Filters filtersForRange(Range range) {
        Filters result = null;
        if( canSearch(range) ){
            result = getSearchableStatsStorage().filters(range.fromDate, range.toDate);
        }
        return (result != null) ? result : Filters.EMPTY;
    }

    @GET
    @Path("/res-dynamic/endoscopeAppType.js")
    @Produces("application/javascript")
    public Response defaultSettings() {
        return noCacheResponse(
                "window.endoscopeAppType = \"" + Properties.getAppType() + "\";\n" +
                "window.endoscopeAppGroup = \"" + Properties.getAppInstance() + "\";\n"
        );
    }
}
