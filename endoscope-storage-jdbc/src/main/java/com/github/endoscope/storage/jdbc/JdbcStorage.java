package com.github.endoscope.storage.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.Histogram;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.storage.jdbc.dto.GroupEntity;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import com.github.endoscope.storage.jdbc.handler.GroupEntityHandler;
import com.github.endoscope.storage.jdbc.handler.StatEntityHandler;
import com.github.endoscope.storage.jdbc.handler.StringHandler;
import com.github.endoscope.util.DateUtil;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;

import static com.github.endoscope.storage.jdbc.ListUtil.emptyIfNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.time.DateUtils.setMilliseconds;
import static org.slf4j.LoggerFactory.getLogger;

public class JdbcStorage implements Storage {
    protected static final Logger log = getLogger(JdbcStorage.class);

    protected QueryRunnerExt run;
    protected GroupEntityHandler groupHandler = new GroupEntityHandler();
    protected StatEntityHandler statHandler = new StatEntityHandler();
    protected StringHandler stringHandler = new StringHandler();
    protected String tablePrefix = "";
    protected static long ONE_HOUR_MILLIS = 360000L;
    protected static long ONE_DAY_MILLIS = 24 * ONE_HOUR_MILLIS;
    protected static int HISTOGRAM_PAGE = 100;
    protected static int HISTOGRAM_PAGE_MAX = 120;

    public JdbcStorage setTablePrefix(String tablePrefix){
        this.tablePrefix = tablePrefix;
        return this;
    }

    /**
     * Accepts following input parameters:
     * - class name of your com.github.endoscope.storage.jdbc.DataSourceProvider implementation
     * - JDBC connection string for com.github.endoscope.basic.ds.BasicDataSourceProvider
     *    (if included on class path)
     * - JNDI name of DataSource object
     * @param initParam
     */
    @Override
    public void setup(String initParam) {
        DataSource ds = DataSourceHelper.findDatasource(initParam);
        if( ds == null ){
            throw new IllegalStateException("Cannot setup storage without DataSource");
        }
        run = new QueryRunnerExt(ds);
    }

    public QueryRunnerExt getRun() {
        return run;
    }

    public void setRun(QueryRunnerExt run) {
        this.run = run;
    }

    @Override
    public String save(Stats stats, String instance, String type) {
        return replace(null, stats, instance, type);
    }

    @Override
    public void cleanup(int daysToKeep, String type) {
        if( daysToKeep <= 0 ){
            log.debug("cleanup disabled - skipping");
            return;
        }
        List<String> groupsToDelete = null;

        Date date = DateUtils.addDays(new Date(), -1 * daysToKeep);
        Timestamp ts = new Timestamp(date.getTime());
        try{
            log.debug("searching for groups to delete. Table prefix:{}, days to keep: {}", tablePrefix, daysToKeep);
            groupsToDelete = run.queryExt(10,
                " SELECT id " +
                " FROM "+tablePrefix+"endoscopeGroup " +
                " WHERE 1=1" +
                "  AND enddate < ?" +
                "  AND startdate < ?" +
                "  AND appType = ?" +
                " LIMIT 10",
                stringHandler, ts, ts, type);
            if( groupsToDelete.isEmpty() ){
                log.debug("nothing to cleanup");
                return;
            }
            log.debug("Found groups to clean: {}", groupsToDelete);

            log.debug("cleaning stats");
            run.update("DELETE FROM "+tablePrefix+"endoscopeStat WHERE groupId in(" + listOfArgs(groupsToDelete.size()) + ")",
                    groupsToDelete.toArray());

            log.debug("cleaning groups");
            run.update("DELETE FROM "+tablePrefix+"endoscopeGroup WHERE id in(" + listOfArgs(groupsToDelete.size()) + ")",
                    groupsToDelete.toArray());

            log.debug("completed cleanup");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cleanup groups: " + groupsToDelete, e);
        }
    }

    @Override
    public String replace(String statsId, Stats stats, String instance, String type) {
        try{
            try(Connection conn = run.getDataSource().getConnection()){
                conn.setAutoCommit(false);
                try{
                    if( isNotBlank(statsId) ){
                        List<String> groups = run.query(conn, "SELECT id FROM "+tablePrefix+"endoscopeGroup WHERE id = ? FOR UPDATE", stringHandler, statsId);
                        if( groups.size() != 1 ){
                            throw new RuntimeException("Failed to lock group with ID: " + statsId + " - ABORTING");
                        }
                        run.update(conn, "DELETE FROM "+tablePrefix+"endoscopeGroup WHERE id = ? ", statsId);
                        run.update(conn, "DELETE FROM "+tablePrefix+"endoscopeStat WHERE groupId = ? ", statsId);
                    }
                    String groupId = UUID.randomUUID().toString();
                    insertGroup(stats, conn, groupId, instance, type);
                    insertStats(stats, conn, groupId);
                    conn.commit();
                    return groupId;
                }catch(Exception e){
                    conn.rollback();
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void insertGroup(Stats stats, Connection conn, String groupId, String instance, String type) throws SQLException {
        int u = run.update(conn,
                "INSERT INTO "+tablePrefix+"endoscopeGroup(id, startDate, endDate, statsLeft, lost, fatalError, appGroup, appType) " +
                "                    values( ?,         ?,       ?,         ?,    ?,          ?,        ?,       ?)",
                groupId,
                new Timestamp(setMilliseconds(stats.getStartDate(), 0).getTime()),
                new Timestamp(setMilliseconds(stats.getEndDate(), 0).getTime()),
                stats.getStatsLeft(),
                stats.getLost(),
                stats.getFatalError(),
                instance,
                type
        );
        if( u != 1 ){
            throw new RuntimeException("Failed to insert stats group. Expected 1 result but got: " + u);
        }
    }

    protected void insertStats( Stats stats, Connection conn, String groupId) throws SQLException {
        Object[][] data = prepareStatsData(groupId, stats);
        int[] result = run.batch(conn,
                //endoscopeStat OR endoscopeDailyStat
                "INSERT INTO " + tablePrefix
                        + "endoscopeStat(id, groupId, parentId, rootId, name, hits, err, max, min, avg, ah10, hasChildren) "
                        + " values(?,?,?,?,?,?,?,?,?,?,?,?)",
                data);
        long errors = Arrays.stream(result)
                .filter( i -> i < 0 && i != Statement.SUCCESS_NO_INFO )
                .count();
        if( errors > 0 ){
            throw new RuntimeException("Failed to insert stats. Got " + errors + " errors");
        }
    }

    protected Object[][] prepareStatsData(String groupId, Stats stats) {
        List<Object[]> resultList = new ArrayList<>();
        prepareStatsData(groupId, null, null, stats.getMap(), resultList);
        return resultList.toArray(new Object[0][0]);
    }

    protected void prepareStatsData(String groupId, String parentId, String rootId, Map<String, Stat> map,
                                  List<Object[]> resultList){
        map.forEach((statName, stat) -> {
            String statId = UUID.randomUUID().toString();
            String fixedRootId = (rootId == null) ? statId : rootId;
            resultList.add(new Object[]{
                    statId, groupId, parentId, fixedRootId, statName,
                    stat.getHits(), stat.getErr(), stat.getMax(), stat.getMin(), stat.getAvg(), stat.getAh10(),
                    stat.getChildren() != null ? 1 : 0
            });
            if( stat.getChildren() != null ){
                prepareStatsData(groupId, statId, fixedRootId, stat.getChildren(), resultList);
            }
        });
    }

    @Override
    public Stats load(String groupId) {
        try {
            GroupEntity group = loadGroup(groupId);
            List<StatEntity> stats = loadGroupStats(groupId);
            restoreGroupStats(group, stats);
            group.setInfo(storageInfo(group));
            return group;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void restoreGroupStats(GroupEntity group, List<StatEntity> stats) {
        List<StatEntity> roots = restoreStatTree(stats);
        roots.forEach( se -> group.getMap().put(se.getName(), se.getStat()) );
    }

    /**
     * Links parent stats to its' parents.
     * returns list of root stats.
     * @param stats
     */
    protected List<StatEntity> restoreStatTree(List<StatEntity> stats) {
        List<StatEntity> roots = new ArrayList<>();
        Map<String, StatEntity> statsById = stats.stream().collect(toMap(se -> se.getId(), se -> se));
        stats.forEach( se -> {
            if( se.getParentId() == null ){
                roots.add(se);
            } else {
                StatEntity parent = statsById.get(se.getParentId());
                parent.getStat().ensureChildrenMap();
                parent.getStat().getChildren().put(se.getName(), se.getStat());
            }
        });
        return roots;
    }

    protected List<StatEntity> loadGroupStats(String id) {
        return loadGroupStats(singletonList(id), null, false);
    }

    protected List<StatEntity> loadGroupStats(List<String> groupIds, boolean topLevelOnly) {
        return loadGroupStats(groupIds, null, topLevelOnly);
    }

    protected List<StatEntity> loadGroupStats(List<String> groupIds, String name) {
        return loadGroupStats(groupIds, name, false);
    }

    protected List<StatEntity> loadGroupStats(List<String> groupIds, String name, boolean topLevelOnly) {
        List<StatEntity> result = new ArrayList<>();
        ListUtil.partition(groupIds, 50).forEach(partition -> {
            try {
                long start = System.currentTimeMillis();

                List args = new ArrayList<>();
                String query = " SELECT " + StatEntityHandler.STAT_FIELDS +
                        " FROM "+tablePrefix+"endoscopeStat ";

                if( isBlank(name) ){
                    query += " WHERE groupId IN (" + listOfArgs(partition.size()) + ")";
                    if( topLevelOnly ){
                        query += " AND parentId is null ";
                    }
                    args.addAll(partition);
                } else {
                    if( topLevelOnly ){
                        query += " WHERE groupId IN (" + listOfArgs(partition.size()) + ")"
                            + " AND parentId is null"
                            + " AND name = ? ";
                    } else {
                        query += "WHERE rootId IN( " +
                            "   SELECT id " +
                            "   FROM "+tablePrefix+"endoscopeStat " +
                            "   WHERE groupId IN (" + listOfArgs(partition.size()) + ")" +
                            "     AND name = ? " +
                            ")";
                    }
                    args.addAll(partition);
                    args.add(name);
                }

                log.debug("Loading group stats");
                List<StatEntity> partitionResult = run.queryExt(200, query, statHandler, args.toArray());
                log.debug("Loaded {} group stats in {}ms", partitionResult.size(), System.currentTimeMillis() - start);

                result.addAll(partitionResult);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return result;
    }

    protected GroupEntity loadGroup(String id) throws SQLException {
        long start = System.currentTimeMillis();
        log.debug("Loading group for id: {}", id);
        List<GroupEntity> groups = run.queryExt(200,
            " SELECT " + GroupEntityHandler.GROUP_FIELDS +
            " FROM "+tablePrefix+"endoscopeGroup " +
            " WHERE id = ? ", groupHandler, id );
        log.debug("Loaded group for id {} in {}ms", id, System.currentTimeMillis() - start);
        if( groups.isEmpty() ){
            return null;
        }
        return groups.get(0);
    }

    @Override
    public List<String> find(Date from, Date to, String instance, String type) {
        List<GroupEntity> groups = findGroupsInRange(from, to, instance, type);
        return extractGroupIds(groups);
    }

    protected List<GroupEntity> findGroupsInRange(Date from, Date to, String appInstance, String appType) {
        try {
            log.debug("Loading groups in range");
            long start = System.currentTimeMillis();

            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());

            // Query performance optimization.
            //   endDate < ? and startDate > ?
            // conditions don't change result but influence the way query optimizer works.
            // It may break contract if group length is longer than search range and at the same time search range
            // is smaller than 1 day. Such configuration should not be use in this app.
            long rangeMilliseconds = to.getTime() - from.getTime();
            if( rangeMilliseconds < ONE_DAY_MILLIS ){
                rangeMilliseconds = ONE_DAY_MILLIS;
            }
            rangeMilliseconds += ONE_HOUR_MILLIS;
            Timestamp distantFromTs = new Timestamp(from.getTime() - rangeMilliseconds);
            Timestamp distantToTs = new Timestamp(to.getTime() + rangeMilliseconds);

            List<GroupEntity> list = run.queryExt(200,
                    " SELECT " + GroupEntityHandler.GROUP_FIELDS +
                    " FROM "+tablePrefix+"endoscopeGroup " +
                    " WHERE endDate >= ? AND endDate < ?" +
                    " AND startDate <= ? AND startDate > ?" +
                     optAppFilterQuery(appInstance, appType) +
                    " ORDER BY startDate", groupHandler, filterBlank(
                            fromTs, distantToTs, toTs, distantFromTs, appInstance, appType)
            );

            log.debug("Loaded {} groups for range: {} to {} in {}ms", list.size(), from, to, System.currentTimeMillis() - start);
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<GroupEntity> findGroupsByIds(List<String> ids) {
        List<GroupEntity> result = new ArrayList<>();

        ids = emptyIfNull(ids).stream().filter( id -> isNotBlank(id) ).collect(toList());
        if( ids.isEmpty() ){
            return result;
        }
        ListUtil.partition(ids, 50).forEach(partition -> {
            try {
                long start = System.currentTimeMillis();
                log.debug("Loading groups by ids - one partition");

                List<GroupEntity> partitionResult = run.queryExt(50,
                        " SELECT " + GroupEntityHandler.GROUP_FIELDS +
                        " FROM "+tablePrefix+"endoscopeGroup " +
                        " WHERE id IN (" + listOfArgs(partition.size()) + ") " +
                        " ORDER BY startDate " ,
                        groupHandler, partition.toArray()
                );

                log.debug("Loaded {} groups by ids in {}ms - one partition", partitionResult.size(), System.currentTimeMillis() - start);
                result.addAll(partitionResult);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return result;
    }

    protected String listOfArgs(int count){
        StringBuilder sb = new StringBuilder("?");
        for(int i=1; i<count; i++){
            sb.append(",?");
        }
        return sb.toString();
    }

    protected String optAppFilterQuery(String appInstance, String appType){
        StringBuilder q = new StringBuilder();
        if( isNotBlank(appInstance) ){
            q.append(" AND appGroup = ? ");
        }
        if( isNotBlank(appType) ){
            q.append(" AND appType = ? ");
        }
        return q.toString();
    }

    protected Object[] filterBlank(Object ... params){
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
                .collect(toList())
                .toArray();
    }

    @Override
    public Filters findFilters(Date from, Date to, String type) {
        try {
            long start = System.currentTimeMillis();

            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());

            List<String> instances;
            List<String> types;
            if( isNotBlank(type) ){
                log.debug("Finding instances for filters");
                instances = run.queryExt(20,
                                " SELECT distinct(appGroup) " +
                                " FROM "+tablePrefix+"endoscopeGroup " +
                                " WHERE endDate >= ? AND startDate <= ? AND appType = ?",
                        stringHandler, fromTs, toTs, type);
                log.debug("Loaded {} instances for filters in {}ms", instances.size(), System.currentTimeMillis() - start);
                types = singletonList(type);
            } else {
                log.debug("Finding instances for filters");
                instances = run.queryExt(20,
                                " SELECT distinct(appGroup) " +
                                " FROM "+tablePrefix+"endoscopeGroup " +
                                " WHERE endDate >= ? AND startDate <= ? ",
                        stringHandler, fromTs, toTs);
                log.debug("Loaded {} instances for filters in {}ms", instances.size(), System.currentTimeMillis() - start);

                start = System.currentTimeMillis();
                log.debug("Finding types for filters");
                types = run.queryExt(20,
                                " SELECT distinct(appType) " +
                                " FROM "+tablePrefix+"endoscopeGroup " +
                                " WHERE endDate >= ? AND startDate <= ? "
                        , stringHandler, fromTs, toTs);
                log.debug("Loaded {} types for filters in {}ms", types.size(), System.currentTimeMillis() - start);
            }

            return new Filters(instances, types, storageInfo(null));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected String storageInfo(Stats stats) {
        StringBuilder info = new StringBuilder();
        info.append("tablePrefix=").append(tablePrefix);
        if( stats != null ){
            info.append(", start=");
            if( stats.getStartDate() != null ){
                info.append(DateUtil.DATE_TIME_GMT.format(stats.getStartDate()));
            }else {
                info.append("null");
            }

            info.append(", end=");
            if( stats.getEndDate() != null ){
                info.append(DateUtil.DATE_TIME_GMT.format(stats.getEndDate()));
            } else {
                info.append("null");
            }
        }
        return info.toString();
    }

    @Override
    public StatDetails loadDetails(String detailsId, List<String> groupIds) {
        List<GroupEntity> groups = findGroupsByIds(groupIds);
        List<StatEntity> stats = loadGroupStats(groupIds, detailsId);
        return createDetails(detailsId, groups, stats);
    }

    @Override
    public StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type) {
        List<GroupEntity> groups = findGroupsInRange(from, to, instance, type);
        List<String> groupIds = extractGroupIds(groups);
        List<StatEntity> stats = loadGroupStats(groupIds, detailsId);
        return createDetails(detailsId, groups, stats);
    }

    @Override
    public Histogram loadHistogram(String detailsId, Date from, Date to, String instance, String type, String lastGroupId){
        List<GroupEntity> allGroups = findGroupsInRange(from, to, instance, type);
        List<GroupEntity> tailGroups = tailAfter(allGroups, lastGroupId);
        List<GroupEntity> groups = limitHistogramSize(tailGroups);

        lastGroupId = (groups.size() == tailGroups.size()) ? null : groups.get(HISTOGRAM_PAGE-1).getId();

        List<String> groupIds = extractGroupIds(groups);
        List<StatEntity> stats = loadGroupStats(groupIds, detailsId, true);//topLevel only

        Histogram result = createHistogram(groups, stats);
        result.setId(detailsId);
        result.setInfo(storageInfo(null));
        result.setLastGroupId(lastGroupId);

        result.setStartDate(allGroups.stream().map(GroupEntity::getStartDate).min(Date::compareTo).orElseGet(() -> new Date()));
        result.setEndDate(allGroups.stream().map(GroupEntity::getEndDate).max(Date::compareTo).orElseGet(() -> new Date()));

        return result;
    }

    protected List<GroupEntity> tailAfter(List<GroupEntity> groups, String lastGroupId) {
        if( lastGroupId == null ){
            return groups;
        }
        List<GroupEntity> nextGroups = new ArrayList();
        final MutableBoolean found = new MutableBoolean(false);
        groups.forEach( g -> {
            if(found.isTrue()) {
                nextGroups.add(g);
            } else if (lastGroupId.equals(g.getId()) ) {
                found.setTrue();
            }
        });
        return nextGroups;
    }

    protected List<GroupEntity> limitHistogramSize(List<GroupEntity> groups) {
        //if there is just few more than don't do the trick
        if( groups.size() <= HISTOGRAM_PAGE_MAX ){
            return groups;
        }
        return groups.subList(0, HISTOGRAM_PAGE);
    }

    protected List<String> extractGroupIds(List<GroupEntity> groups) {
        return groups.stream()
                    .map( g-> g.getId())
                    .collect(toList());
    }

    protected StatDetails createDetails(String detailsId, List<GroupEntity> groups, List<StatEntity> stats) {
        StatDetails result = new StatDetails(detailsId, null);
        result.setInfo(storageInfo(null));
        groups.forEach( group -> {
            List<StatEntity> groupStats = stats.stream()
                    .filter( s -> group.getId().equals(s.getGroupId()) )
                    .collect(toList());
            restoreGroupStats(group, groupStats);

            Stat details = group.getMap().get(detailsId);

            result.add(details);
        });
        if( result.getMerged() == null ){
            result.setMerged(Stat.emptyStat());
        }
        return result;
    }

    protected Histogram createHistogram(List<GroupEntity> groups, List<StatEntity> topLevelStats) {
        Histogram result = new Histogram();
        groups.forEach( group -> {
            Stat details = topLevelStats.stream()
                    .filter( s -> group.getId().equals(s.getGroupId()) )
                    .filter( s -> s != null )
                    .map( s-> s.getStat())
                    .findFirst()
                    .orElseGet(() -> Stat.emptyStat());
            result.add(details, group.getStartDate(),group.getEndDate());
        });
        return result;
    }

    @Override
    public Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type) {
        List<GroupEntity> groups = findGroupsInRange(from, to, instance, type);
        List<String> groupIds = extractGroupIds(groups);
        List<StatEntity> stats = loadGroupStats(groupIds, topLevelOnly);

        Stats result = new Stats();
        groups.forEach( group -> {
            List<StatEntity> groupStats = stats.stream()
                    .filter( s -> group.getId().equals(s.getGroupId()) )
                    .collect(toList());
            restoreGroupStats(group, groupStats);
            result.merge(group, !topLevelOnly);
        });
        result.setInfo(storageInfo(result));
        return result;
    }
}
