package com.github.endoscope.example;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class TheStateless {
    @Inject
    TheErrorThrower nextBean;

    public String process(int level) {
        try{
            nextBean.process();
        }catch(Exception e){
            //silent
        }
        SleepUtil.randomSleep();
        return "OK";
    }
}
