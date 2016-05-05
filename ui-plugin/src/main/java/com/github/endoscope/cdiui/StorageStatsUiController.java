package com.github.endoscope.cdiui;

import com.github.endoscope.Endoscope;
import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
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
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/endoscope/storage")
public class StorageStatsUiController extends StaticResourceController{
    private static final Logger log = getLogger(StorageStatsUiController.class);

    private JsonUtil jsonUtil = new JsonUtil();

    protected Response noCacheResponse( Object entity ) {
        CacheControl cc = new CacheControl();
        cc.setNoCache( true );
        cc.setMaxAge( -1 );
        cc.setMustRevalidate( true );

        return Response.ok( entity ).cacheControl( cc ).build();
    }

    @GET
    @Path("ui/data/top")
    @Produces("application/json")
    public Response top(@QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("past") String past) {
        Stats stats = topLevel(toLong(from), toLong(to), toLong(past));
        return noCacheResponse(jsonUtil.toJson(stats.getMap()));
    }

    @GET
    @Path("ui/data/details")
    @Produces("application/json")
    public Response details(@QueryParam("id") String id, @QueryParam("from") String from, @QueryParam("to") String to,
                           @QueryParam("past") String past){
        StatDetails child = stat(id, toLong(from), toLong(to), toLong(past));
        return noCacheResponse(jsonUtil.toJson(child));
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
            if( storage == null ){
                throw new RuntimeException("Storage not supported");
            }
        }catch(ClassCastException e){
            throw new RuntimeException("Range search is not supported");
        }
        return storage;
    }
}
