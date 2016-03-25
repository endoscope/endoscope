package org.endoscope.example;

import javax.ejb.Stateless;

@Stateless
public class TheStateless {
    public String process(int level) {
        SleepUtil.randomSleep();
        return "OK";
    }
}
