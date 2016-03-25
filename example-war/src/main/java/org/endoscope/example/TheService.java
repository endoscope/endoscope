package org.endoscope.example;

import javax.inject.Inject;

public class TheService {
    @Inject
    TheBean service1;

    @Inject
    TheApplicationScoped service2;

    public String process(int level) {
        SleepUtil.randomSleep();
        level--;
        for( int i=0; i<level; i++){
            service1.randomTimes(level);
            service2.process(level);
        }
        return "OK";
    }
}
