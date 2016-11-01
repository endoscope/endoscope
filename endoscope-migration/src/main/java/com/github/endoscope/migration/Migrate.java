package com.github.endoscope.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.storage.aggr.AggregatedStorage;
import com.github.endoscope.storage.gzip.AggregatedGzipStorage;
import com.github.endoscope.storage.jdbc.AggregatedJdbcStorage;
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
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class Migrate {
    private static final Logger log = getLogger("MIGRATE");

    public static void main(String[] args) throws Exception {
        Map<String, String> config = loadConfiguration(args[0]);
        String operation = config.get("@operation");

        if( "aggrDBtoGzip".equals(operation) ){
            migrateDBtoGzip(config);
        } else if( "aggrDBtoDB".equals(operation) ){
            migrateDBtoDB(config);
        } else {
            log.error("Unknown operation name: {}", operation);
            System.exit(1);
        }
    }

    public static void migrateDBtoDB(Map<String, String> config) throws Exception {
        checkUtcTimeZone();

        String jdbcSrc = config.get("@jdbcSrc");
        checkJdbcConnectionString(jdbcSrc, "@jdbcSrc");

        String jdbcDst = config.get("@jdbcDst");
        checkJdbcConnectionString(jdbcDst, "@jdbcDst");

        log.info("Initializing existing data storage - input DS");
        JdbcStorage existing = new JdbcStorage();
        existing.setup(jdbcSrc);

        log.info("Initializing output aggregated data storage - runs in aggregation mode only and doesn't store original stats again");
        AggregatedJdbcStorage aggregated = new AggregatedJdbcStorage();
        aggregated.setup(jdbcDst);
        aggregated.setAggregateOnly(true);

        migrateAll(config, existing, aggregated);
    }

    public static void migrateDBtoGzip(Map<String, String> config) throws Exception {
        checkUtcTimeZone();

        String jdbcSrc = config.get("@jdbcSrc");
        checkJdbcConnectionString(jdbcSrc, "@jdbcSrc");

        String gzipDst = config.get("@gzipDst");
        checkExistingDirectory(gzipDst, "@gzipDst");

        log.info("Initializing existing data storage - input DS");
        JdbcStorage existing = new JdbcStorage();
        existing.setup(jdbcSrc);

        log.info("Initializing output aggregated data storage - runs in aggregation mode only and doesn't store original stats again");
        AggregatedGzipStorage aggregated = new AggregatedGzipStorage();
        aggregated.setup(gzipDst);
        aggregated.setAggregateOnly(true);

        migrateAll(config, existing, aggregated);
    }

    private static void checkJdbcConnectionString(String value, String cfgKey) {
        if ( isBlank(value) || !value.startsWith("jdbc:")) {
            log.error("Configuration property {} is not valid JDBC connection string", cfgKey);
            System.exit(1);
        }
    }

    private static void checkExistingDirectory(String dir, String cfgKey) {
        if( isBlank(dir) || !new File(dir).exists() || !new File(dir).isDirectory() ){
            log.error( "Configuration property {} is not valid existing directory", cfgKey );
            System.exit(1);
        }
    }

    private static void checkUtcTimeZone() {
        if( !"UTC".equals(System.getProperty("user.timezone", "")) ){
            log.error( "Please set VM argument with proper timezone: -Duser.timezone=UTC. DB operations might work incorrectly without it.");
            System.exit(1);
        }
    }

    private static void migrateAll(Map<String, String> config, JdbcStorage existing, AggregatedStorage aggregated) throws Exception {

        Date from = DATE_TIME_GMT.parse(defaultIfBlank(config.get("@from"), "@from date not set in config"));
        Date to   = DATE_TIME_GMT.parse(defaultIfBlank(config.get("@to"), "@to date not set in config"));

        log.info("Processing stats in range: {} to {}", formatDate(from), formatDate(to));

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
                "  \"@operation\": \"aggrDBtoGzip | aggrDBtoDB\",\n" +
                "  \"@jdbcSrc\": \"jdbc connection string\",\n" +
                "  \"@jdbcDst\": \"jdbc connection string\",\n" +
                "  \"@gzipDst\": \"storage directory\",\n" +
                "  \"@operation\": \"DBtoGzip | DBtoDB\",\n" +
                "  \"@from\": \"lower bound of start date in format: yyyy-MM-dd HH:mm:ss\",\n" +
                "  \"@to\": \"upper bound of end date in format: yyyy-MM-dd HH:mm:ss\"\n" +
                "}\n\n" +
                "Please remember that both: start and end date must match given range.\n" +
                "Stats are processed in start date order starting with next ID after <last stat ID> for given type.\n" +
                "If <type> key is not set then such stats are ignored.",
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
