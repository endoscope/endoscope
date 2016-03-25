package org.endoscope.example;

import javax.inject.Inject;

public class TheBean {
    @Inject
    TheStateless service;

    public String randomTimes(int level) {
        SleepUtil.randomSleep();
        level--;
        for(int i = 0; i< System.currentTimeMillis() % 5; i++){
            service.process(level);
        }
        return "OK";
    }
}
