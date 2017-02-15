package com.github.endoscope.util;


import com.github.endoscope.core.Stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class HistogramUtil {
    /**
     * Try to create evenly distributed list of stats.
     * Main purpose is to limit number of data we need to load in order to show readable histogram.
     * Sometimes we have 3000 stats in range and it takes significant time to load it all.
     * UX is much better when we show as fast as possible 100 points on chart.
     *
     * @param points
     * @return
     */
    public static <T extends Stats> List<T> reduce(int points, List<T> stats){
        if( points < 2 ){
            throw new RuntimeException("At least 2 points are required");
        }

        if( stats == null || stats.isEmpty() ){
            return stats;
        }

        List<Tick<T>> ticks = stats.stream()
                .map( s-> new Tick<T>(s.getStartDate(), s.getEndDate(), s) )
                .sorted(Comparator.comparingLong(Tick::getPoint))
                .collect(toList());


        List<T> result = new ArrayList<>();

        Tick<T> first = ticks.get(0);
        Long min = first.getPoint();

        Tick<T> last = ticks.get(ticks.size() - 1);
        Long max = last.getPoint();

        long pointLength = (max - min)/(points-1);

        //first is obvious
        result.add(first.getStats());

        //find middle points best matching evenly distributes values in range (min, max)
        for(int i=1; i<points-1; i++){
            Tick<T> best = null;
            long bestDistance = Long.MAX_VALUE;
            long current = min + i * pointLength;

            //find tick best matching current point
            for( Tick t : ticks){
                long distance = Math.abs(current - t.getPoint());
                if( distance < bestDistance ){
                    best = t;
                    bestDistance = distance;
                }
            }
            if( best != null && !result.contains(best.getStats()) ){
                result.add(best.getStats());
            }
        }

        //last is obvious
        if( !result.contains(last.getStats()) ){
            result.add(last.getStats());
        }

        return result;
    }

    private static class Tick<T> {
        private Long point;
        private T stats;

        public Tick(Date startDate, Date endDate, T stats){
            startDate = defaultIfNull(startDate, endDate);
            endDate = defaultIfNull(endDate, startDate);
            this.point = (startDate.getTime() + endDate.getTime())/2;
            this.stats = stats;
        }

        public Long getPoint() {
            return point;
        }

        public void setPoint(Long point) {
            this.point = point;
        }

        public T getStats() {
            return stats;
        }

        public void setStats(T stats) {
            this.stats = stats;
        }
    }
}
