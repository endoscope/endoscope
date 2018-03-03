package com.github.endoscope.example;

import javax.ejb.Stateless;

@Stateless
public class TheErrorThrower {
    private static long count = 0;
    public String process() {
        if((count++)%2 == 0){
            throw new RuntimeException("error simulation");
        }
        return "OK";
    }
}
