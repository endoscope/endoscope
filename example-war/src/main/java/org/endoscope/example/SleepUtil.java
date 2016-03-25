package org.endoscope.example;

import java.util.Random;

public class SleepUtil {
    private static Random r = new Random();
    public static void randomSleep() {
        try {
            Thread.sleep(r.nextInt(10));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
