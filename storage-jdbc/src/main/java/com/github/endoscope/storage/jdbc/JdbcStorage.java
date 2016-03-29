package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.StatsStorage;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;


public class JdbcStorage extends StatsStorage {
    private static final Logger log = getLogger(JdbcStorage.class);
    protected QueryRunner run;
    protected ListOfMapRSHandler handler = new ListOfMapRSHandler();

    public JdbcStorage(String initParam){
        super(initParam);

        //initParam = "java:jboss/datasources/ExampleDS"
        DataSource ds = DataSourceHelper.findDatasource(initParam);
        run = new QueryRunner(ds);
    }

    @Override
    public String save(Stats stats) {
        try{
            try(Connection conn = run.getDataSource().getConnection()){
                conn.setAutoCommit(false);
                try{
                    String groupId = UUID.randomUUID().toString();
                    int u = run.update(conn, "INSERT INTO endoscopeGroup(id, startDate, endDate, statsLeft, lost, fatalError) values(?,?,?,?,?,?)",
                            groupId, stats.getStartDate(), stats.getEndDate(),
                            stats.getStatsLeft(), stats.getLost(), stats.getFatalError()
                    );
                    if( u != 1 ){
                        throw new RuntimeException("Failed to insert stats group. Expected 1 result but got: " + u);
                    }

                    Object[][] data = prepareStatsData(groupId, stats);
                    int[] result = run.batch(conn, "INSERT INTO endoscopeStat(id, groupId, parentId, rootId, name, hits, max, min, avg, ah10, hasChildren) values(?,?,?,?,?,?,?,?,?,?,?)", data);
                    int inserts = Arrays.stream(result).sum();
                    if( inserts != data.length ){
                        throw new RuntimeException("Failed to insert stats. Expected " + data.length + " but got: " + inserts);
                    }

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

    private Object[][] prepareStatsData(String groupId, Stats stats) {
        List<Object[]> list = new ArrayList<>();
        process(groupId, null, null, stats.getMap(), list);
        return list.toArray(new Object[0][0]);
    }

    private void process(String groupId, String parentId, String rootId, Map<String, Stat> map, List<Object[]> list){
        map.forEach((statName, stat) -> {
            String statId = UUID.randomUUID().toString();
            list.add(new Object[]{
                    statId, groupId, parentId, rootId, statName,
                    stat.getHits(), stat.getMax(), stat.getMin(), stat.getAvg(), stat.getAh10(),
                    stat.getChildren() != null
            });
            if( stat.getChildren() != null ){
                String currentRoot = rootId == null ? statId : rootId;
                process(groupId, statId, currentRoot, stat.getChildren(), list);
            }
        });
    }
}
