package com.github.endoscope.example;

import java.util.Random;

public class SleepUtil {
    private static Random r = new Random();
    public static void randomSleep() {
        try {
            Thread.sleep(r.nextInt(5) * 50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
