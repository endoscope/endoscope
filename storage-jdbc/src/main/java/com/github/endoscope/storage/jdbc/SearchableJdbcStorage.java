package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.SearchableStatsStorage;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.jdbc.dto.GroupEntity;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import com.github.endoscope.storage.jdbc.handler.GroupEntityHandler;
import com.github.endoscope.storage.jdbc.handler.StatEntityHandler;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

public class SearchableJdbcStorage extends JdbcStorage implements SearchableStatsStorage {
    private static final Logger log = getLogger(SearchableJdbcStorage.class);
    private GroupEntityHandler groupHandler = new GroupEntityHandler();
    private StatEntityHandler statHandler = new StatEntityHandler();
    private static final int IN_SIZE = 100;
    private InParamsUtil inParamsUtil = new InParamsUtil(IN_SIZE, null);


    public SearchableJdbcStorage(String initParam){
        super(initParam);
    }

    @Override
    public Stats topLevel(Date from, Date to) {
        List<GroupEntity> groups = findGroupsInRange(from, to);

        Stats result = collectTopLevel(groups);
        return result;
    }

    private List<GroupEntity> findGroupsInRange(Date from, Date to) {
        try {
            long start = System.currentTimeMillis();

            Timestamp fromTs = new Timestamp(from.getTime());
            Timestamp toTs = new Timestamp(to.getTime());
            List<GroupEntity> list = run.queryExt(200,
                    " SELECT " + GroupEntityHandler.GROUP_FIELDS +
                    " FROM endoscopeGroup " +
                    " WHERE startDate >= ? AND endDate <= ? " +
                    " ORDER BY startDate", groupHandler, fromTs, toTs);

            log.info("Loaded {} groups for range: {} to {} in {}ms",
                    list.size(), from, to, System.currentTimeMillis() - start);
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Stats collectTopLevel(List<GroupEntity> groups) {
        Stats result = new Stats();
        if( !groups.isEmpty() ){
            //DB have a limit of elements in IN clause
            ListUtil.partition(groups, IN_SIZE).forEach( partition -> loadTopLevel(partition) );
            groups.forEach(g -> result.merge(g, false));
        }
        return result;
    }

    private void loadTopLevel(List<GroupEntity> groups) {
        try {
            Map<String, GroupEntity> groupMap = groups.stream().collect(toMap(g -> g.getId(), g -> g));
            long start = System.currentTimeMillis();
            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeStat " +
                    " WHERE parentId is null AND groupId in(" + inParamsUtil.getInParams() + ")",
                    statHandler,
                    inParamsUtil.fillMissingValues(groupMap.keySet().toArray())
            );
            log.info("Loaded {} top level stats for partition size: {} in {}ms",
                    stats.size(), groups.size(), System.currentTimeMillis() - start);
            stats.forEach( se -> {
                GroupEntity g = groupMap.get(se.getGroupId());
                g.getMap().put(se.getName(), se.getStat());
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StatDetails stat(String rootName, Date from, Date to) {
        StatDetails result = new StatDetails(rootName, null);

        List<GroupEntity> groups = findGroupsInRange(from, to);

        //DB have a limit of elements in IN clause
        ListUtil.partition(groups, IN_SIZE).forEach(partition -> {
            loadTree(partition, rootName);
            partition.forEach(g -> {
                Stat details = g.getMap().get(rootName);
                result.add(details, g.getStartDate(),g.getEndDate());
            });
        });
        if( result.getMerged() == null ){
            result.setMerged(Stat.EMPTY_STAT);
        }
        return result;
    }

    private void loadTree(List<GroupEntity> partition, String rootName) {
        Map<String, GroupEntity> groupById = partition.stream().collect(toMap(g -> g.getId(), g -> g));
        Collection<String> groupIds = groupById.keySet();
        try {
            long start = System.currentTimeMillis();
            List<StatEntity> stats = run.queryExt(200,
                    " SELECT " + StatEntityHandler.STAT_FIELDS +
                    " FROM endoscopeStat " +
                    " WHERE rootId IN(" +
                    "     SELECT rootId " +
                    "     FROM endoscopeStat " +
                    "     WHERE parentId is null AND name = ? AND groupId IN(" + inParamsUtil.getInParams() + ") " +
                    " )",
                    statHandler,
                    inParamsUtil.fillMissingValues(new Object[]{rootName}, groupIds.toArray())
            );
            log.info("Loaded {} stats for partition of size {} in {}ms",
                    stats.size(), partition.size(), System.currentTimeMillis() - start);

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
}
