package org.endoscope.core;

import java.util.Date;
import java.util.Random;

import static java.util.stream.IntStream.range;

public class StatTestUtil {
    private static Random r = new Random();

    public static Stat buildRandomStat(int maxChildren){
        Stat s = new Stat();
        s.setHits(rlong());
        s.setMax(rlong());
        s.setMin(rlong());
        s.setAvg(rlong());
        s.setParentCount(rlong());
        s.setAvgParent(rdouble());

        if( maxChildren > 0){
            int numChildren = r.nextInt(maxChildren);
            if( numChildren > 0 ){
                s.ensureChildrenMap();
                range(0,numChildren)
                        .forEach(i -> s.getChildren().put("c" + i, buildRandomStat(maxChildren-1)));
            }
        }

        return s;
    }

    private static long rlong(){
        return Math.abs(r.nextLong()) % 100000000;
    }

    private static double rdouble(){
        return Math.abs((r.nextDouble()));
    }

    public static Stats buildRandomStats(int maxChildren){
        Stats s = new Stats();
        s.setStatsLeft(rlong());
        s.setLost(rlong());
        s.setFatalError("msg" + rlong());
        s.setStartDate(new Date(rlong()));
        s.setEndDate(new Date(rlong()));

        int numChildren = r.nextInt(maxChildren);
        if( numChildren > 0 ){
            range(0,numChildren)
                    .forEach(i -> s.getMap().put("c" + i, buildRandomStat(maxChildren-1)));
        }
        return s;
    }
}
