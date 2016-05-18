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

    public SearchableJdbcStorage(String initParam){
        super(initParam);
    }

    @Override
    public Stats topLevel(Date from, Date to) {
        Stats result = new Stats();

        List<GroupEntity> groups = findGroupsInRange(from, to);
        if( !groups.isEmpty() ){
            loadTopLevel(groups, from, to);
            groups.forEach(g -> result.merge(g, false));
        }
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

    private void loadTopLevel(List<GroupEntity> groups, Date from, Date to) {
        try {
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
                    "   WHERE startDate >= ? AND endDate <= ? " +
                    " )",
                    statHandler, fromTs, toTs
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

        loadTree(groups, rootName, from, to);
        groups.forEach(g -> {
            Stat details = g.getMap().get(rootName);
            result.add(details, g.getStartDate(),g.getEndDate());
        });
        if( result.getMerged() == null ){
            result.setMerged(Stat.EMPTY_STAT);
        }
        return result;
    }

    private void loadTree(List<GroupEntity> partition, String rootName, Date from, Date to) {
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
                    "         WHERE startDate >= ? AND endDate <= ? " +
                    "     ) " +
                    " )",
                    statHandler, rootName, fromTs, toTs
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
