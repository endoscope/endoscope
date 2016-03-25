package org.endoscope.cdiui;

import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class StaticResourceController {
    private static final Logger log = getLogger(StaticResourceController.class);

    private static final String DEV_DIR;

    static {
        DEV_DIR = System.getProperty("org.endoscope.dev.res.dir");
    }

    @GET
    @Path("/ui")// this path is related in PopulateUiDataFilter
    public Response ui() throws FileNotFoundException {
        return uiResource("index.html");
    }

    @GET
    @Path("/ui/res/{path:.*}")
    public Response uiResource(@PathParam("path") String path) throws FileNotFoundException {
        InputStream resourceAsStream = null;
        if( DEV_DIR != null ){
            resourceAsStream = new FileInputStream(new File(DEV_DIR + "/" + path));
        } else {
            resourceAsStream = getClass().getResourceAsStream("/res/" + path);
        }
        if( resourceAsStream == null ){
            return Response.status(404).build();
        }
        return Response.ok(resourceAsStream)
                .type(mediaType(path))
                .cacheControl(oneDayCache()).build();
    }

    private String mediaType(String path){
        if( path.endsWith(".js") )return "application/javascript";
        if( path.endsWith(".css") )return "text/css";
        if( path.endsWith(".html"))return "text/html";

        log.warn("can't find media type for path: {}", path);
        return null;
    }
    
    private CacheControl oneDayCache() {
        CacheControl cc = new CacheControl();
        //Set max age to one day
        cc.setMaxAge(86400);
        return cc;
    }
}
