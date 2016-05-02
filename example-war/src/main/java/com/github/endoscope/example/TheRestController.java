package com.github.endoscope.example;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static org.slf4j.LoggerFactory.getLogger;

@Path("/controller")
public class TheRestController {
    private static final Logger log = getLogger(TheRestController.class);

    @Inject
    TheService theService;

    @GET
    @Path("/process")
    @Produces({ "application/json" })
    public String process(@QueryParam("sleepMs") String sleepMs){
        log.info("Processing");
        int level = 3;
        for( int i=0; i<level; i++){
            theService.process(level);
        }
        if( sleepMs != null ){
            sleep(Long.parseLong(sleepMs));
        }
        return "{\"result\":\"OK\"}";
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
