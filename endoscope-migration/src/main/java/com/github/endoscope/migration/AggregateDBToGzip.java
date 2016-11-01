package com.github.endoscope.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.storage.gzip.AggregatedGzipStorage;
import com.github.endoscope.storage.jdbc.JdbcStorage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.endoscope.util.DateUtil.DATE_TIME_GMT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class AggregateDBToGzip {
    private static final Logger log = getLogger("MIGRATE");

    public static void main(String[] args) throws Exception {
        if( !"UTC".equals(System.getProperty("user.timezone", "")) ){
            log.error( "Please set VM argument with proper timezone: -Duser.timezone=UTC" );
            System.exit(1);
        }
        if( args.length < 1 || !args[0].startsWith("jdbc:") ){
            log.error( "Expected at least one argument: GZIP files storage directory" );
            System.exit(1);
        }
        if( args.length < 2 || !new File(args[1]).exists() || !new File(args[1]).isDirectory() ){
            log.error( "Expected GZIP files storage directory as second argument" );
            System.exit(1);
        }

        String fileName = "migration.cfg.json";
        if( args.length > 2 ){
            fileName = args[2];
        }
        Map<String, String> config = loadConfiguration(fileName);
        Date from = DATE_TIME_GMT.parse(defaultIfBlank(config.get("@from"), "@from date not set in config"));
        Date to   = DATE_TIME_GMT.parse(defaultIfBlank(config.get("@to"), "@to date not set in config"));
        log.info("Processing stats in range: {} to {}", formatDate(from), formatDate(to));

        log.info("Initializing existing data storage - input DS");
        JdbcStorage existing = new JdbcStorage().setTablePrefix("");
        existing.setup(args[0]);

        log.info("Initializing output aggregated data storage - runs in aggregation mode only and doesn't store original stats again");
        AggregatedGzipStorage aggregated = new AggregatedGzipStorage();

        aggregated.setAggregateOnly(true);
        aggregated.setup(args[1]);

        Filters filters = existing.findFilters(from, to, null);
        log.info("Found {} types to migrate: {}", filters.getTypes().size(), filters.getTypes());

        filters.getTypes().forEach( type -> {
            if( !config.containsKey(type) ){
                log.info("Type: {} not found in configuration - IGNORING", type);
                return;
            }
            String lastId = config.getOrDefault(type, "");
            migrateType(lastId, from, to, existing, aggregated, type);
        });

        log.info("Done");
    }

    private static void migrateType(String lastId, Date from, Date to,
                                    JdbcStorage existing, Storage aggregated,
                                    String type) {
        log.info("Loading all IDS of stats for type: {}", type );
        List<String> statsIds = existing.find(from, to, null, type);
        statsIds = removeUntilLastId(lastId, statsIds);

        long allCount = statsIds.size();
        log.info("Found {} stats to migrate", allCount);

        MutableInt count = new MutableInt(0);
        statsIds.forEach( statsId -> {
            count.increment();
            log.info("Loading {} of {} stat: {}, {}", count.getValue(), allCount, statsId, type);
            Stats stats = existing.load(statsId);
            log.info("Updating aggregated stats with data from period: {} to {}", formatDate(stats.getStartDate()), formatDate(stats.getEndDate()));

            //we don't need instance name as we save aggregated stats only which ignores this parameter anyway
            aggregated.save(stats, null, type);
        });
        log.info("Finished migrating type: {}", type );
    }

    private static List<String> removeUntilLastId(String lastId, List<String> statsIds) {
        if( isNotBlank(lastId) ){
            log.info("Found ID to start with {} - skipping all ID until this id (including it)", lastId );
            List<String> remaining = new ArrayList();
            boolean found = false;
            for( String s : statsIds ){
                if( found ){
                    remaining.add(s);
                }
                if( !found && s.equals(lastId) ){
                    found = true;
                }
            }
            log.info("Skipped: {}, of: {}. Id left to process: {}", statsIds.size() - remaining.size(), statsIds.size(), remaining.size());
            statsIds = remaining;
        } return statsIds;
    }

    private static String formatDate(Date startDate) {
        return DATE_TIME_GMT.format(startDate);
    }

    private static Map<String, String> loadConfiguration(String fileName) {
        File file = new File(fileName);
        log.info("Loading configuration JSON from file {} \n" +
                "Example config: {\n" +
                "  \"<type1>\": \"<empty or last processed stat ID of type1>\",\n" +
                "  \"<typeN>\": \"<empty or last processed stat ID of typeN>\",\n" +
                "  \"@from\": \"lower bound of start date in format: yyyy-MM-dd HH:mm:ss\",\n" +
                "  \"@to\": \"upper bound of end date in format: yyyy-MM-dd HH:mm:ss\"\n" +
                "}\n\n" +
                "Please remember that both: start and end date must match given range.\n" +
                "Stats are processed in start order starting with next ID after <last stat ID> for given type.\n" +
                "If <type> key is not in configuration then such stats are ignored.",
                file.getAbsolutePath()
        );
        Map<String, String> config;
        try{
            String startWithJSON = FileUtils.readFileToString(file);
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(startWithJSON, HashMap.class);
            log.info("Loaded skip configuration from startWith.txt file: {}", config);
        }catch(Exception e){
            log.error("Failed to load configuration file: {} - using empty config", file.getAbsolutePath());
            config = new HashMap<>();
        }
        return config;
    }
}
