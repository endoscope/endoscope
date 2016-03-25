package org.endoscope.storage.jdbc;

import org.endoscope.core.Stat;
import org.endoscope.core.Stats;
import org.endoscope.storage.SearchableStatsStorage;
import org.endoscope.storage.StatDetails;
import org.endoscope.storage.StatHistory;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class SearchableJdbcStorage extends JdbcStorage implements SearchableStatsStorage {
    private static final Logger log = getLogger(SearchableJdbcStorage.class);

    public SearchableJdbcStorage(String initParam){
        super(initParam);
    }

    @Override
    public Stats topLevel(Date from, Date to) {
        Stats result = new Stats();
        List<Group> groups = findByDates(from, to);
        groups.forEach(g -> {
            loadTopLevel(g);
            result.merge(g, false);
        });
        return result;
    }

    private List<Group> findByDates(Date from, Date to) {
        try {
            //TODO switch to ResultSetHandler<Map<Long, Person>> h = new BeanMapdHandler<Long, Person>(Person.class, "id");
            List<Map<String, Object>> list = run.query(
                    " select " +
                    "   id, startDate, endDate, statsLeft, lost, fatalError " +
                    " from endoscopeGroup " +
                    " where startDate >= ? and endDate <= ? order by startDate", handler, from, to);
            return list.stream()
                    .map( data -> toGroup(data))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTopLevel(Group group) {
        try {
            //TODO switch to ResultSetHandler<Map<Long, Person>> h = new BeanMapdHandler<Long, Person>(Person.class, "id");
            List<Map<String, Object>> stats = run.query(
                    " select " +
                    "  name, hits, max, min, avg, ah10, hasChildren " +
                    " from endoscopeStat " +
                    " where parentId is null and groupId = ?", handler, group.getId());

            stats.forEach( data -> {
                String statName = data.get("name").toString();
                boolean hasChildren = (Boolean)data.get("hasChildren");
                Stat stat = toStat(data);
                if( hasChildren ){
                    stat.ensureChildrenMap();
                }
                group.getMap().put(statName, stat);
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Stat toStat(Map<String, Object> data) {
        Stat stat = new Stat();
        stat.setHits(((Number)data.get("hits")).longValue());
        stat.setMax(((Number)data.get("max")).longValue());
        stat.setMin(((Number)data.get("min")).longValue());
        stat.setAvg(((Number)data.get("avg")).longValue());
        stat.setAh10(((Number)data.get("ah10")).longValue());
        return stat;
    }

    private Group toGroup(Map<String, Object> data){
        Group group = new Group();

        group.setId(data.get("id").toString());
        group.setStartDate((Date)data.get("startDate"));
        group.setEndDate((Date)data.get("endDate"));
        group.setStatsLeft(((Number)data.get("statsLeft")).longValue());
        group.setLost(((Number)data.get("lost")).longValue());
        group.setFatalError(data.get("fatalError").toString());

        return group;
    }

    @Override
    public StatDetails stat(String id, Date from, Date to) {
        StatDetails result = new StatDetails(null);
        result.setId(id);

        List<Group> groups = findByDates(from, to);
        groups.forEach(g -> {
            Stat details = loadTree(g.getId(), id);

            if( details != null ){
                if( result.getMerged() == null ){
                    result.setMerged(details.deepCopy(true));
                } else {
                    result.getMerged().merge(details, true);
                }
                //TODO merge to no more than 100 points
                result.getHistogram().add(
                        new StatHistory(
                                details,
                                g.getStartDate(),
                                g.getEndDate()
                        ));

            }
        });
        if( result.getMerged() == null ){
            result.setMerged(new Stat());
        }
        return result;
    }

    private Stat loadTree(String groupId, String rootName) {
        try {
            //TODO switch to ResultSetHandler<Map<Long, Person>> h = new BeanMapdHandler<Long, Person>(Person.class, "id");
            List<Map<String, Object>> rootDataList = run.query(
                    " select " +
                    "  id, hits, max, min, avg, ah10 " +
                    " from endoscopeStat " +
                    " where parentId is null and groupId = ? and name = ?", handler, groupId, rootName);

            Map<String, Object> rootData = rootDataList.get(0);
            if( rootDataList.size() != 1 || rootData.get("id") == null ){
                log.warn("Unable to load root stat for group: {} and name: {}", groupId, rootName);
                return null;
            }
            String rootId = rootData.get("id").toString();
            Stat root = toStat(rootData);

            loadChildren(groupId, rootId, root);

            return root;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadChildren(String groupId, String rootId, Stat root) throws SQLException {
        //TODO switch to ResultSetHandler<Map<Long, Person>> h = new BeanMapdHandler<Long, Person>(Person.class, "id");
        List<Map<String, Object>> stats = run.query(
                " select " +
                "  parentId, name, hits, max, min, avg, ah10 " +
                " from endoscopeStat " +
                " where groupId = ? and rootId = ?", handler, groupId, rootId);

        Map<String, List<StatInfo>> statsByParentId = new HashMap<>();
        stats.forEach( data -> {
            String statParentId = data.get("parentId").toString();
            StatInfo statInfo = new StatInfo();
            statInfo.setName(data.get("name").toString());
            statInfo.setStat(toStat(data));

            List<StatInfo> list = statsByParentId.get(statParentId);
            if( list == null ){
                list = new ArrayList<>();
                statsByParentId.put(statParentId, list);
            }
            list.add(statInfo);
        });

        addChildren(statsByParentId, rootId, root);
    }

    private void addChildren(Map<String, List<StatInfo>> statsByParentId, String parentId, Stat parent){
        List<StatInfo> children = statsByParentId.get(parentId);
        if( children != null && children.size() > 0){
            parent.ensureChildrenMap();
            children.forEach(c -> {
                parent.getChildren().put(c.getName(), c.getStat());
                addChildren(statsByParentId, c.getId(), c.getStat());
            });
        }
    }
}
