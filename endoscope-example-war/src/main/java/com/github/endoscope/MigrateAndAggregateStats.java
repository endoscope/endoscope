package com.github.endoscope;

import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.jdbc.AggregatedJdbcStorage;
import com.github.endoscope.storage.jdbc.JdbcStorage;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.List;

public class MigrateAndAggregateStats {
    public static void main(String[] args) throws Exception {
        if( args.length != 1 || !args[0].startsWith("jdbc:") ){
            System.err.println( "Expected exactly one argument: JDBC connection string" );
            System.exit(1);
        }

        System.out.println("Initializing existing data storage - input DS");
        // source - not aggregated stats
        JdbcStorage existing = new JdbcStorage().setTablePrefix("");
        existing.setup(args[0]);

        System.out.println("Initializing aggregated data storage - output DS");
        // output aggregated stats
        // we'll run it in aggregation mode only - do not store again original stats
        AggregatedJdbcStorage aggregated = new AggregatedJdbcStorage();

        aggregated.setAggregateOnly(true);
        aggregated.setup(args[0]);

        Date from = new Date(0);//1970
        Date to = new Date();//now

        Filters filters = existing.findFilters(from, to, null);
        System.out.println("Found " + filters.getTypes().size() + " types to migrate: " + filters.getTypes());

        filters.getTypes().forEach( type -> {
            System.out.println("Loading IDS of stats for type: " + type );
            String instance = null; // all
            List<String> statsIds = existing.find(from, to, instance, type);
            System.out.println("Found " + statsIds.size() + " stats to migrate");

            MutableInt count = new MutableInt(0);
            statsIds.forEach( statsId -> {
                count.increment();
                System.out.println("Loading " + count.getValue() + " stat of type " + type);
                Stats stats = existing.load(statsId);
                System.out.println("Saving " + count.getValue() + " stat of type " + type);

                //we don't need instance name as we save aggregated stats only which ignores this parameter anyway
                aggregated.save(stats, null, type);
            });
            System.out.println("Finished migrating type: " + type );
        });

        System.out.println("Done");
    }
}
