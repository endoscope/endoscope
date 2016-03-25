package org.endoscope.example;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/controller")
public class TheRestController {
    private static final Logger log = getLogger(TheRestController.class);

    @Inject
    TheService theService;

    @GET
    @Path("/process")
    @Produces({ "application/json" })
    public String process() {
        log.info("Processing");
        int level = 5;
        for( int i=0; i<level; i++){
            theService.process(level);
        }
        return "{\"result\":\"OK\"}";
    }
}
