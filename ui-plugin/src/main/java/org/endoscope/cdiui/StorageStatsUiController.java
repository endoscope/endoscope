package org.endoscope.cdiui;

import org.endoscope.Endoscope;
import org.endoscope.core.Stat;
import org.endoscope.core.Stats;
import org.endoscope.util.JsonUtil;
import org.endoscope.storage.SearchableStatsStorage;
import org.endoscope.storage.StatDetails;
import org.slf4j.Logger;

import javax.ws.rs.*;
import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/endoscope/storage")
public class StorageStatsUiController extends StaticResourceController{
    private static final Logger log = getLogger(StorageStatsUiController.class);

    private JsonUtil jsonUtil = new JsonUtil();
    private TopLevelStatsSerializer topLevelStatsSerializer = new TopLevelStatsSerializer();

    @GET
    @Path("ui/data/top")
    @Produces("application/json")
    public String top(@QueryParam("from") String from, @QueryParam("to") String to, @QueryParam("past") String past) {
        Stats stats = topLevel(toLong(from), toLong(to), toLong(past));
        return topLevelStatsSerializer.serialize(stats.getMap());
    }

    //TODO remove after switching to details with histogram
    @GET
    @Path("ui/data/sub/{id}")
    @Produces("application/json")
    public String sub(@PathParam("id") String id, @QueryParam("from") String from, @QueryParam("to") String to,
                      @QueryParam("past") String past){
        StatDetails child = stat(id, toLong(from), toLong(to), toLong(past));
        return jsonUtil.toJson(child.getMerged());
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
        return from != null && to != null;// && from.compareTo(to) < 0;
    }

    //TODO cache?
    private Stats topLevel(Long from, Long to, Long past){
        if( past != null ){
            to = System.currentTimeMillis();
            from = to - past;
        }
        if( !isRange(from, to) ){
            return Endoscope.processStats(stats -> stats.deepCopy(false) );
        }
        return getSearchableStatsStorage().topLevel(new Date(from), new Date(to));
    }

    //TODO cache?
    private StatDetails stat(String id, Long from, Long to, Long past){
        if( past != null ){
            to = System.currentTimeMillis();
            from = to - past;
        }
        if( !isRange(from, to) ){
            Stat stat = Endoscope.processStats(stats -> stats.getMap().get(id).deepCopy());
            return new StatDetails(stat);
        }
        return getSearchableStatsStorage().stat(id, new Date(from), new Date(to));
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
