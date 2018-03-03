package com.github.endoscope.storage.jdbc.handler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.endoscope.core.Stat;
import com.github.endoscope.storage.jdbc.dto.StatEntity;
import org.apache.commons.dbutils.ResultSetHandler;

public class StatEntityHandler implements ResultSetHandler<List<StatEntity>> {
    //index is safer as column names are sometimes upper cased and sometimes not - depends on DB
    public static final String STAT_FIELDS = "id, groupId, parentId, name, hits, err, max, min, avg, ah10, hasChildren";
    //                                         1,       2,        3,    4,    5,   6,   7,   8,   9,   10,          11

    public List<StatEntity> handle(ResultSet rs) throws SQLException {
        List<StatEntity> result = new ArrayList<>();
        while (rs.next()) {
            StatEntity se = new StatEntity();

            se.setId(rs.getString(1));
            se.setGroupId(rs.getString(2));
            se.setParentId(rs.getString(3));
            se.setName(rs.getString(4));

            Stat stat = se.getStat();
            stat.setHits(rs.getLong(5));
            stat.setErr(rs.getLong(6));
            stat.setMax(rs.getLong(7));
            stat.setMin(rs.getLong(8));
            stat.setAvg(rs.getLong(9));
            stat.setAh10(rs.getLong(10));

            Long hasChildren = rs.getLong(11);
            if( hasChildren > 0 ){
                stat.ensureChildrenMap();
            }
            result.add(se);
        }
        return result;
    }
}
