package org.endoscope.cdiui;

import org.endoscope.Endoscope;
import org.endoscope.core.Stat;
import org.endoscope.util.JsonUtil;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
@Path("/endoscope/current")
public class InMemoryStatsUiController extends StaticResourceController {
    private static final Logger log = getLogger(InMemoryStatsUiController.class);

    private JsonUtil jsonUtil = new JsonUtil();
    private TopLevelStatsSerializer topLevelStatsSerializer = new TopLevelStatsSerializer();

    @GET
    @Path("ui/data/top")
    @Produces("application/json")
    public String top() {
        return Endoscope.processStats(stats -> topLevelStatsSerializer.serialize(stats.getMap()));
    }

    @GET
    @Path("ui/data/sub/{id}")
    @Produces("application/json")
    public String sub(@PathParam("id") String id){
        //TODO add top level stats cache and/or invalidation token
        // There is a problem with data consistency between UI and server side.
        // After save stats gets cleared. Because of that stats presented in the browser might no longer reflect
        // stats held on the server. Due to that fact requested ID might no longer be available.

        return Endoscope.processStats(stats -> {
            Stat child = stats.getMap().get(id);
            return jsonUtil.toJson(child);
        });
    }
}
