package com.github.endoscope.storage.jdbc;

import com.github.endoscope.storage.aggr.AggregatedStorage;

import static java.util.Arrays.asList;

public class AggregatedJdbcStorage extends AggregatedStorage {
    @Override
    public void setup(String initParam) {
        defaultStorage  = new JdbcStorage().setTablePrefix("");
        defaultStorage.setup(initParam);

        dailyStorage    = new JdbcStorage().setTablePrefix("day_");
        weeklyStorage   = new JdbcStorage().setTablePrefix("week_");
        monthlyStorage  = new JdbcStorage().setTablePrefix("month_");

        //save DB connection resources and re-use the same DS for aggregated storage
        asList( dailyStorage, weeklyStorage, monthlyStorage)
                .forEach( s -> ((JdbcStorage)s).setRun( ((JdbcStorage)defaultStorage).getRun() ) );
    }
}
