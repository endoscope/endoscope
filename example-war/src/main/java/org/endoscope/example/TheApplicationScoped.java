package org.endoscope.example;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class TheApplicationScoped {
    @Inject
    TheStateless service;

    public String process(int level) {
        SleepUtil.randomSleep();
        level--;
        for( int i=0; i<level; i++){
            service.process(level);
        }
        return "OK";
    }

}
