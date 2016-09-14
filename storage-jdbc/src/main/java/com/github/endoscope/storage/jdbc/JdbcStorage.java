package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.StatsStorage;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.time.DateUtils.setMilliseconds;
import static org.slf4j.LoggerFactory.getLogger;


public class JdbcStorage extends StatsStorage {
    private static final Logger log = getLogger(JdbcStorage.class);
    protected QueryRunnerExt run;
    protected String appInstance;
    protected String appType;

    public JdbcStorage(String initParam){
        super(initParam);

        //initParam = "java:jboss/datasources/ExampleDS"
        DataSource ds = DataSourceHelper.findDatasource(initParam);
        if( ds == null ){
            throw new IllegalStateException("Cannot setup storage without DataSource");
        }
        run = new QueryRunnerExt(ds);

        appInstance = abbreviate(Properties.getAppInstance(), 100);
        appType = abbreviate(Properties.getAppType(), 100);
        log.info("Endoscope JDBC storage will use app instance: {}, type: {}", appInstance, appType);
    }

    @Override
    public String save(Stats stats) {
        try{
            try(Connection conn = run.getDataSource().getConnection()){
                conn.setAutoCommit(false);
                try{
                    String groupId = UUID.randomUUID().toString();
                    insertGroup(stats, conn, groupId);
                    insertStats(stats, conn, groupId);
                    beforeSaveCommit(stats, conn, groupId);
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

    protected void beforeSaveCommit(Stats stats, Connection conn, String groupId) throws SQLException{
    }

    private void insertStats(Stats stats, Connection conn, String groupId) throws SQLException {
        insertStats("endoscopeStat", stats, conn, groupId);
    }

    protected void insertStats(String tableName, Stats stats, Connection conn, String groupId) throws SQLException {
        Object[][] data = prepareStatsData(groupId, stats);
        int[] result = run.batch(conn,
                //endoscopeStat OR endoscopeDailyStat
                "INSERT INTO " + tableName + "(id, groupId, parentId, rootId, name, hits, max, min, avg, ah10, hasChildren) " +
                "                       values( ?,       ?,        ?,      ?,    ?,    ?,   ?,   ?,   ?,    ?,           ?)",
                data);
        long errors = Arrays.stream(result)
                .filter( i -> i < 0 && i != Statement.SUCCESS_NO_INFO )
                .count();
        if( errors > 0 ){
            throw new RuntimeException("Failed to insert stats. Got " + errors + " errors");
        }
    }

    private void insertGroup(Stats stats, Connection conn, String groupId) throws SQLException {
        int u = run.update(conn,
                "INSERT INTO endoscopeGroup(id, startDate, endDate, statsLeft, lost, fatalError, appGroup, appType) " +
                "                    values( ?,         ?,       ?,         ?,    ?,          ?,        ?,       ?)",
                groupId,
                new Timestamp(setMilliseconds(stats.getStartDate(), 0).getTime()),
                new Timestamp(setMilliseconds(stats.getEndDate(), 0).getTime()),
                stats.getStatsLeft(),
                stats.getLost(),
                stats.getFatalError(),
                appInstance,
                appType
        );
        if( u != 1 ){
            throw new RuntimeException("Failed to insert stats group. Expected 1 result but got: " + u);
        }
    }

    private Object[][] prepareStatsData(String groupId, Stats stats) {
        List<Object[]> list = new ArrayList<>();
        process(groupId, null, null, stats.getMap(), list);
        return list.toArray(new Object[0][0]);
    }

    private void process(String groupId, String parentId, String rootId, Map<String, Stat> map, List<Object[]> list){
        map.forEach((statName, stat) -> {
            String statId = UUID.randomUUID().toString();
            String fixedRootId = (rootId == null) ? statId : rootId;
            list.add(new Object[]{
                    statId, groupId, parentId, fixedRootId, statName,
                    stat.getHits(), stat.getMax(), stat.getMin(), stat.getAvg(), stat.getAh10(),
                    stat.getChildren() != null ? 1 : 0
            });
            if( stat.getChildren() != null ){
                process(groupId, statId, fixedRootId, stat.getChildren(), list);
            }
        });
    }
}
