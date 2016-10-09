package com.github.endoscope.util;

import com.github.endoscope.core.Stats;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;

public class AggregateStatsUtil {
    public static Stats buildDailyStats(Stats stats){
        Stats daily = new Stats();
        daily.setFatalError(stats.getFatalError());

        //daily stats start at midnight (inclusive) and end at midnight of next day (exclusive)
        daily.setStartDate(DateUtil.startOfADay(stats.getStartDate()));
        daily.setEndDate(DateUtils.addDays(daily.getStartDate(), 1));

        //rewrite root stats only
        stats.getMap().forEach( (id, stat) -> {
            //skip nested stats
            daily.getMap().put(id, stat.deepCopy(false));
        });
        return daily;
    }

    public static String buildDailyGroupId(String appType, Stats dailyStats){
        return buildDailyGroupId(appType, dailyStats.getStartDate());
    }

    public static String buildDailyGroupId(String appType, Date startDate){
        return appType + "-" + DateUtil.DATE_ONLY_GMT.format(startDate);
    }
}
