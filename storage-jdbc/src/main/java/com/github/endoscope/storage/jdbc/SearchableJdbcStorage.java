package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.SearchableStatsStorage;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.jdbc.dto.GroupEntity;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import com.github.endoscope.storage.jdbc.handler.GroupEntityHandler;
import com.github.endoscope.storage.jdbc.handler.StatEntityHandler;
import com.github.endoscope.storage.jdbc.handler.StringHandler;
import com.github.endoscope.util.AggregateStatsUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class SearchableJdbcStorage extends JdbcStorage implements SearchableStatsStorage {
    private static final Logger log = getLogger(SearchableJdbcStorage.class);
    private GroupEntityHandler groupHandler = new GroupEntityHandler();
    private StatEntityHandler statHandler = new StatEntityHandler();
    private StringHandler stringHandler = new StringHandler();

    public SearchableJdbcStorage(String initParam){
        super(initParam);
    }

    @Override
    public Stats topLevel(Date from, Date to, String appInstance, String appType) {
        Stats result = new Stats();

        List<GroupEntity> groups = findGroupsInRange(from, to, appInstance, appType);
        if( !groups.isEmpty() ){
            loadTopLevel(groups, from, to, appInstance, appType);
            groups.forEach(g -> result.merge(g, false));
        }
        return result;
    }

    private List<GroupEntity> findGroupsInRange(Date from, Date to, String appInstance, String appType) {
        try {
            log.debug("Loading groups");
            long start = System.currentTimeMillis();

            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());
            List<GroupEntity> list = run.queryExt(200,
                    " SELECT " + GroupEntityHandler.GROUP_FIELDS +
                    " FROM endoscopeGroup " +
                    " WHERE startDate >= ? AND endDate <= ? " + optAppFilterQuery(appInstance, appType) +
                    " ORDER BY startDate", groupHandler, filterBlank(fromTs, toTs, appInstance, appType)
            );

            log.debug("Loaded {} groups for range: {} to {} in {}ms", list.size(), from, to, System.currentTimeMillis() - start);
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTopLevel(List<GroupEntity> groups, Date from, Date to, String appInstance, String appType) {
        //TODO temporary solution until we permanently switch to this version
        if( "#daily#".equals(appInstance) ){
            loadTopLevelDaily(groups, from, to, appType);
        } else {
            loadTopLevelDetailed(groups, from, to, appInstance, appType);
        }
    }

    private void loadTopLevelDetailed(List<GroupEntity> groups, Date from, Date to, String appInstance, String appType) {
        try {
            log.debug("Loading stats");

            Map<String, GroupEntity> groupMap = groups.stream().collect(toMap(g -> g.getId(), g -> g));
            long start = System.currentTimeMillis();
            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());
            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeStat " +
                    " WHERE parentId is null AND groupId in(" +
                    "   SELECT id " +
                    "   FROM endoscopeGroup " +
                    "   WHERE startDate >= ? AND endDate <= ? " + optAppFilterQuery(appInstance, appType) +
                    " )",
                    statHandler, filterBlank(fromTs, toTs, appInstance, appType)
            );
            log.debug("Loaded {} top level stats for partition size: {} in {}ms", stats.size(), groups.size(), System.currentTimeMillis() - start);
            stats.forEach( se -> {
                GroupEntity g = groupMap.get(se.getGroupId());
                g.getMap().put(se.getName(), se.getStat());
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTopLevelDaily(List<GroupEntity> groups, Date from, Date to, String appType) {
        try {
            log.debug("Loading top level daily stats");

            String dailyGroupIdFrom = AggregateStatsUtil.buildDailyGroupId(appType, from);
            String dailyGroupIdTo = AggregateStatsUtil.buildDailyGroupId(appType, to);

            Map<String, GroupEntity> groupMap = groups.stream().collect(toMap(g -> g.getId(), g -> g));
            long start = System.currentTimeMillis();
            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeDailyStat " +
                    " WHERE parentId is null AND groupId >= ? AND groupId <= ?",
                    statHandler, filterBlank(dailyGroupIdFrom, dailyGroupIdTo)
            );
            log.debug("Loaded {} top level daily stats for partition size: {} in {}ms", stats.size(), groups.size(), System.currentTimeMillis() - start);
            stats.forEach( se -> {
                GroupEntity g = groupMap.get(se.getGroupId());
                g.getMap().put(se.getName(), se.getStat());
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Stats loadDailyStatsForUpdate(String dailyGroupId) {
        try {
            Stats dailyStats = new Stats();

            log.debug("Loading daily stats");
            long start = System.currentTimeMillis();
            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeDailyStat " +
                    " WHERE parentId is null AND groupId = ?" +
                    " FOR UPDATE ",
                    statHandler, filterBlank(dailyGroupId)
            );
            log.debug("Loaded daily stats {} in {}ms", stats.size(), System.currentTimeMillis() - start);

            stats.forEach( se -> dailyStats.getMap().put(se.getName(), se.getStat()) );
            return dailyStats;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDailyStats(String dailyGroupId){
        try{
            log.debug("Deleting daily stats");
            long start = System.currentTimeMillis();
            run.update(" DELETE FROM endoscopeDailyStat WHERE groupId = ?", dailyGroupId);
            log.debug("Deleted daily stats {} in {}ms", System.currentTimeMillis() - start);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StatDetails stat(String rootName, Date from, Date to, String appInstance, String appType) {
        StatDetails result = new StatDetails(rootName, null);

        List<GroupEntity> groups = findGroupsInRange(from, to, appInstance, appType);

        loadTree(groups, rootName, from, to, appInstance, appType);
        groups.forEach(g -> {
            Stat details = g.getMap().get(rootName);
            result.add(details, g.getStartDate(),g.getEndDate());
        });
        if( result.getMerged() == null ){
            result.setMerged(Stat.emptyStat());
        }
        return result;
    }

    private void loadTree(List<GroupEntity> partition, String rootName, Date from, Date to, String appInstance, String appType) {
        Map<String, GroupEntity> groupById = partition.stream().collect(toMap(g -> g.getId(), g -> g));
        try {
            long start = System.currentTimeMillis();
            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());

            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeStat " +
                    " WHERE rootId IN(" +
                    "     SELECT rootId " +
                    "     FROM endoscopeStat " +
                    "     WHERE parentId is null AND name = ? AND groupId IN(" +
                    "         SELECT id " +
                    "         FROM endoscopeGroup " +
                    "         WHERE startDate >= ? AND endDate <= ? " + optAppFilterQuery(appInstance, appType) +
                    "     ) " +
                    " )",
                    statHandler, filterBlank(rootName, fromTs, toTs, appInstance, appType)
            );
            log.debug("Loaded {} stats for partition of size {} in {}ms", stats.size(), partition.size(), System.currentTimeMillis() - start);

            Map<String, StatEntity> statsById = stats.stream().collect(toMap( se -> se.getId(), se -> se));
            stats.forEach( se -> {
                if( se.getParentId() == null ){
                    GroupEntity g = groupById.get(se.getGroupId());
                    g.getMap().put(rootName, se.getStat());
                } else {
                    StatEntity parent = statsById.get(se.getParentId());
                    parent.getStat().getChildren().put(se.getName(), se.getStat());
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Filters filters(Date from, Date to) {

        try {
            long start = System.currentTimeMillis();

            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());

            List<String> groups = run.queryExt(20,
                    " SELECT distinct(appGroup) " +
                            " FROM endoscopeGroup " +
                            " WHERE startDate >= ? AND endDate <= ? ",
                    stringHandler, fromTs, toTs);

            log.debug("Loaded {} groups in {}ms", groups.size(), System.currentTimeMillis() - start);
            start = System.currentTimeMillis();

            List<String> types = run.queryExt(20,
                    " SELECT distinct(appType) " +
                            " FROM endoscopeGroup " +
                            " WHERE startDate >= ? AND endDate <= ? "
                            , stringHandler, fromTs, toTs);

            log.debug("Loaded {} types in {}ms", types.size(), System.currentTimeMillis() - start);

            Filters filters = new Filters(groups, types);
            return filters;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String optAppFilterQuery(String appInstance, String appType){
        StringBuilder q = new StringBuilder();
        if( isNotBlank(appInstance) ){
            q.append(" and appGroup = ? ");
        }
        if( isNotBlank(appType) ){
            q.append(" and appType = ? ");
        }
        return q.toString();
    }

    private Object[] filterBlank(Object ... params){
        return Arrays.stream(params)
                .filter( p -> {
                    if(p == null ){
                        return false;
                    }
                    if( p instanceof String ){
                        return isNotBlank((String)p);
                    }
                    return true;
                })
                .collect(Collectors.toList())
                .toArray();
    }

    protected void beforeSaveCommit(Stats stats, Connection conn, String groupId) throws SQLException {
        Stats daily = AggregateStatsUtil.buildDailyStats(stats);
        String dailyGroupId = AggregateStatsUtil.buildDailyGroupId(appType, daily);

        //keep in mind we are in transaction here and this is shared data that may
        //be modified from different threads - if it fail we'll repeat in 5 minutes
        Stats merged = loadDailyStatsForUpdate(dailyGroupId);
        merged.merge(daily, false);
        deleteDailyStats(dailyGroupId);
        insertStats("endoscopeDailyStat", merged, conn, groupId);
    }
}
