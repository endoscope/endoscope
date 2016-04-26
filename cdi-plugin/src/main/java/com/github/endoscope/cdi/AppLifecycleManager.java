package com.github.endoscope.cdi;

import com.github.endoscope.Endoscope;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Why do we need this bean?
 * On application un-deployment - in application container - stats processor thread doesn't get terminated.
 * When we deploy application again next thread is started. We need to detect this situation and stop no longer
 * used thread. Due to different class loaders we cannot reference previous thread.
 *
 * In standalone we don't use it. When JVM stops thread stops as well.
 */
@EndoscopeStartup
@ApplicationScoped
public class AppLifecycleManager {
    private static final Logger log = getLogger(AppLifecycleManager.class);

    @PostConstruct
    public void postConstruct(){
        log.debug("Started Endoscope CDI app lifecycle manager");
    }

    @PreDestroy
    public void preDestroy(){
        log.debug("Detected Endoscope CDI app shutdown");
        Endoscope.stopStatsProcessorThread();
    }

    /*
        Those don't work in CDI 1.0
        We start this bean in CdiExtension and dedicated annotation: EndoscopeStartup

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
    }
    */
}
