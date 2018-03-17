package com.github.endoscope.storage.clob;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.endoscope.core.Stat;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import com.github.endoscope.util.JsonUtil;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.IOUtils;

public class ClobStatEntityHandler implements ResultSetHandler<List<StatEntity>> {
    //index is safer as column names are sometimes upper cased and sometimes not - depends on DB
    public static final String STAT_FIELDS =           "id, groupId, name, hits, err, max, min, avg, hasChildren, children";
    public static final String STAT_FIELDS_TOP_LEVEL = "id, groupId, name, hits, err, max, min, avg, hasChildren";
    //                                                   1,       2,    3,    4,   5,   6,   7,   8,           9,       10

    private boolean topLevelOnly = false;
    private JsonUtil jsonUtil = new JsonUtil();

    public ClobStatEntityHandler(boolean topLevelOnly){
        this.topLevelOnly = topLevelOnly;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatMapWrapper {
        private Map<String, Stat> map;

        public Map<String, Stat> getMap() {
            return map;
        }

        public void setMap(Map<String, Stat> map) {
            this.map = map;
        }
    }

    public List<StatEntity> handle(ResultSet rs) throws SQLException {
        List<StatEntity> result = new ArrayList<>();
        while (rs.next()) {
            StatEntity se = new StatEntity();

            se.setId(rs.getString(1));
            se.setGroupId(rs.getString(2));
            se.setParentId(null);
            se.setName(rs.getString(3));

            Stat stat = se.getStat();
            stat.setHits(rs.getLong(4));
            stat.setErr(rs.getLong(5));
            stat.setMax(rs.getLong(6));
            stat.setMin(rs.getLong(7));
            stat.setAvg(rs.getLong(8));
            Long hasChildren = rs.getLong(9);
            if( hasChildren > 0 ){
                if( topLevelOnly ){
                    stat.ensureChildrenMap();
                } else {
                    //not supported by Postgresql driver - we need to use regular string
                    // Clob clob = rs.getClob(10);
                    // String json = readString(clob);
                    String json = rs.getString(10);
                    StatMapWrapper data = jsonUtil.fromJson(StatMapWrapper.class, json);
                    stat.setChildren(data.getMap());
                }
            }
            result.add(se);
        }
        return result;
    }

    private String readString(Clob clob) throws SQLException {
        try {
            return IOUtils.toString(clob.getCharacterStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
