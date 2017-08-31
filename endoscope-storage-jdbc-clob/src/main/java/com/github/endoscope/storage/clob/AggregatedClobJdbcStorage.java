package com.github.endoscope.storage.clob;

import com.github.endoscope.storage.aggr.AggregatedStorage;

import static java.util.Arrays.asList;

public class AggregatedClobJdbcStorage extends AggregatedStorage {
    @Override
    public void setup(String initParam) {
        defaultStorage  = new ClobJdbcStorage().setTablePrefix("clob_");
        defaultStorage.setup(initParam);

        dailyStorage    = new ClobJdbcStorage().setTablePrefix("clob_day_");
        weeklyStorage   = new ClobJdbcStorage().setTablePrefix("clob_week_");
        monthlyStorage  = new ClobJdbcStorage().setTablePrefix("clob_month_");

        //save DB connection resources and re-use the same DS for aggregated storage
        asList( dailyStorage, weeklyStorage, monthlyStorage)
                .forEach( s -> ((ClobJdbcStorage)s).setRun( ((ClobJdbcStorage)defaultStorage).getRun() ) );
    }
}
