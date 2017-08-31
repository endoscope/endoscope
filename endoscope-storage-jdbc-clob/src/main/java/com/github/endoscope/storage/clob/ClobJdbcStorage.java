package com.github.endoscope.storage.clob;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.jdbc.JdbcStorage;
import com.github.endoscope.storage.jdbc.ListUtil;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import com.github.endoscope.util.JsonUtil;
import org.slf4j.Logger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class ClobJdbcStorage extends JdbcStorage {
    private static final Logger log = getLogger(ClobJdbcStorage.class);
    protected JsonUtil jsonUtil = new JsonUtil();

    protected ClobStatEntityHandler pgStatHandler = new ClobStatEntityHandler(false);
    protected ClobStatEntityHandler pgStatHandlerTopLevel = new ClobStatEntityHandler(true);


    public ClobJdbcStorage(){
        setTablePrefix("");
    }

    public JdbcStorage setTablePrefix(String tablePrefix){
        return super.setTablePrefix("clob_" + tablePrefix);
    }

    protected void insertStats(Stats stats, Connection conn, String groupId) throws SQLException {
        //one record per each top level stat and all children as a JSON

        final String sql = "INSERT INTO " + tablePrefix + "endoscopeStat(id, groupId, rootId, name, hits, max, min, avg, ah10, hasChildren, children) values(?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stats.getMap().forEach((statName, stat) -> {
                try {

                    String statId = UUID.randomUUID().toString();

                    stmt.setObject(1, statId); //id
                    stmt.setObject(2, groupId); //groupId
                    stmt.setObject(3, statId);//rootId
                    stmt.setObject(4, statName);
                    stmt.setObject(5, stat.getHits());
                    stmt.setObject(6, stat.getMax());
                    stmt.setObject(7, stat.getMin());
                    stmt.setObject(8, stat.getAvg());
                    stmt.setObject(9, stat.getAh10());
                    stmt.setObject(10, stat.getChildren() != null ? 1 : 0);

                    String json = getJsonData(stat);
                    //not supported by Postgresql driver - we need to use regular string
                    // Clob clob = conn.createClob();
                    // clob.setString(1, json);
                    // stmt.setClob(11, clob);
                    stmt.setString(11, json);

                    stmt.addBatch();

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            try{
                int[] result = stmt.executeBatch();
                long errors = Arrays.stream(result)
                        .filter(i -> i < 0 && i != Statement.SUCCESS_NO_INFO)
                        .count();
                if (errors > 0) {
                    throw new RuntimeException("Failed to insert stats. Got " + errors + " errors");
                }
            }catch(java.sql.SQLException e){
                printBatchErrors(e);
                throw e;
            }
        }
    }

    private void printBatchErrors(SQLException e) {
        int hardLimit = 10;
        while( e != null && hardLimit > 0){
            log.error(e.getMessage());
            e = e.getNextException();
            hardLimit--;
        }
        if( hardLimit == 0){
            log.error("...and some more");
        }
    }

    private String getJsonData(Stat stat) {
        ClobStatEntityHandler.StatMapWrapper data = new ClobStatEntityHandler.StatMapWrapper();
        if (stat != null) {
            data.setMap(stat.getChildren());
        }
        return jsonUtil.toJson(data);
    }

    protected List<StatEntity> loadGroupStats(List<String> groupIds, String name, boolean topLevelOnly) {
        ClobStatEntityHandler handler = topLevelOnly ? pgStatHandlerTopLevel : pgStatHandler;
        String fields = topLevelOnly ? ClobStatEntityHandler.STAT_FIELDS_TOP_LEVEL : ClobStatEntityHandler.STAT_FIELDS;

        List<StatEntity> result = new ArrayList<>();
        ListUtil.partition(groupIds, 50).forEach(partition -> {
            try {
                long start = System.currentTimeMillis();

                List args = new ArrayList<>();
                String query = " SELECT " + fields + " FROM " + tablePrefix + "endoscopeStat WHERE groupId IN (" + listOfArgs(partition.size()) + ")";
                args.addAll(partition);

                if (isNotBlank(name)) {
                    query += " AND name = ? ";
                    args.add(name);
                }

                log.debug("Loading group stats");
                List<StatEntity> partitionResult = run.queryExt(200, query, handler, args.toArray());
                log.debug("Loaded {} group stats in {}ms", partitionResult.size(), System.currentTimeMillis() - start);

                result.addAll(partitionResult);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return result;
    }
}
