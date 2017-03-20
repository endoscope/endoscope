package com.github.endoscope.cdiui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.github.endoscope.Endoscope;
import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.Histogram;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.StatHistory;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;
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
                        @QueryParam("instance") String instance,
                        @QueryParam("type") String type,
                        @QueryParam("reset") boolean reset) {
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);

        if( reset ){
            log.info("Resetting current stats");
            Endoscope.resetStats();
        }

        Stats stats = topLevelForRange(new Range(from, to, past, instance, type));
        return noCacheResponse(jsonUtil.toJson(stats));
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
                            @QueryParam("instance") String instance,
                            @QueryParam("type") String type){
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);
        StatDetails details = detailsForRange(id, new Range(from, to, past, instance, type));
        return noCacheResponse(jsonUtil.toJson(details));
    }

    @GET
    @Path("/data/histogram")
    @Produces("application/json")
    public Response histogram(@QueryParam("id") String id,
                            @QueryParam("from") String fromS,
                            @QueryParam("to") String toS,
                            @QueryParam("past") String pastS,
                            @QueryParam("instance") String instance,
                            @QueryParam("type") String type,
                            @QueryParam("lastGroupId") String lastGroupId){
        Long from = toLong(fromS), to = toLong(toS), past = toLong(pastS);
        lastGroupId = StringUtils.trimToNull(lastGroupId);
        Histogram histogram = histogramForRange(id, new Range(from, to, past, instance, type), lastGroupId);
        return noCacheResponse(jsonUtil.toJson(histogram));
    }

    private static class Range {
        public Range(Long from, Long to, Long past, String instance, String type){
            this.instance = instance;
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
        String instance;
        String type;
        boolean includeCurrent = true;
    }

    private Stats topLevelForRange(Range range) {
        Stats result;
        if( canSearch(range) ){
            result = getStorage().loadAggregated(true, range.fromDate, range.toDate, range.instance, range.type);
            if( inMemoryInRange(range) ){
                Stats current = topLevelInMemory();
                result.merge(current, false);
                result.setInfo("Added in-memory data. Original info: " + result.getInfo());
            }
        } else{
            result = topLevelInMemory();
            result.setInfo("in-memory data only. Original info: " + result.getInfo());
        }
        return result;
    }

    private StatDetails detailsForRange(String id, Range range) {
        StatDetails result;
        if( canSearch(range) ){
            result = getStorage().loadDetails(id, range.fromDate, range.toDate, range.instance, range.type);
            if( inMemoryInRange(range) ){
                StatDetails current = detailsInMemory(id);
                if( current != null ){
                    //we don't want to merge not set stats as it would reset min value to 0
                    result.getMerged().merge(current.getMerged(), true);
                }
                result.setInfo("Added in-memory data. Original info: " + result.getInfo());
            }
        } else {
            result = detailsInMemory(id);
            result.setInfo("in-memory data only. Original info: " + result.getInfo());
        }
        return (result != null) ? result : new StatDetails(id, Stat.emptyStat());
    }

    private Histogram histogramForRange(String id, Range range, String lastGroupId) {
        Histogram result;
        if( canSearch(range) ){
            result = getStorage().loadHistogram(id, range.fromDate, range.toDate, range.instance, range.type, lastGroupId);
            if( inMemoryInRange(range) ){
                Histogram current = histogramInMemory(id);
                if( current != null ){
                    result.getHistogram().addAll(current.getHistogram());
                }
                result.setInfo("Added in-memory data. Original info: " + result.getInfo());
            }
        } else {
            result = histogramInMemory(id);
            result.setInfo("in-memory data only. Original info: " + result.getInfo());
        }
        return (result != null) ? result : new Histogram(id);
    }

    private boolean inMemoryInRange(Range range){
        return range.includeCurrent
                && (isBlank(range.instance) || StringUtils.equals(range.instance, Properties.getAppInstance()))
                && (isBlank(range.type) || StringUtils.equals(range.type, Properties.getAppType()));
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
            s = s.deepCopy();
            StatDetails result = new StatDetails(id, s);
            return result;
        });
    }

    private Histogram histogramInMemory(String id) {
        return Endoscope.processStats(stats -> {
            Stat s = stats.getMap().get(id);
            if( s == null ){//might happen when stats get saved and/or reset in the mean time
                return null;
            }
            Histogram result = new Histogram(id);
            result.getHistogram().add(
                    new StatHistory(s, stats.getStartDate(), new Date())
            );
            return result;
        });
    }

    private Storage getStorage() {
        Storage storage;
        try{
            storage = Endoscope.getStatsStorage();
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
            filters = getStorage().findFilters(range.fromDate, range.toDate, null);
        }
        if( filters == null){
            filters = new Filters(null, null);
        }
        filters.setInstances(addDefaultAndSort(filters.getInstances(), Properties.getAppInstance()));
        filters.setTypes(    addDefaultAndSort(filters.getTypes(),     Properties.getAppType()));
        return noCacheResponse(jsonUtil.toJson(filters));
    }

    private List<String> addDefaultAndSort(List<String> list, String defaultValue){
        if( list == null ){
            list = new ArrayList<>();
        }
        list = list.stream()
                .filter(element -> element != null)
                .collect(Collectors.toList());

        if( !list.contains(defaultValue) ){
            list.add(defaultValue);
        }
        Collections.sort(list);
        return list;
    }

    @GET
    @Path("/res-dynamic/endoscopeAppType.js")
    @Produces("application/javascript")
    public Response defaultSettings() {
        return noCacheResponse(
                "window.endoscopeAppType = \"" + Properties.getAppType() + "\";\n" +
                "window.endoscopeAppInstance = \"" + Properties.getAppInstance() + "\";\n"
        );
    }
}
