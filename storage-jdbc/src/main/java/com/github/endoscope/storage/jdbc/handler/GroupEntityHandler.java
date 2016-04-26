package com.github.endoscope.storage.jdbc.handler;

import com.github.endoscope.storage.jdbc.dto.GroupEntity;
import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupEntityHandler implements ResultSetHandler<List<GroupEntity>> {
    public static final String GROUP_FIELDS = "id, startDate, endDate, statsLeft, lost, fatalError";
    //                                          1,         2,       3,         4,    5,          6

    public List<GroupEntity> handle(ResultSet rs) throws SQLException {
        List<GroupEntity> result = new ArrayList<>();
        while (rs.next()) {
            GroupEntity g = new GroupEntity();

            g.setId(rs.getString(1));
            g.setStartDate(rs.getTimestamp(2));
            g.setEndDate(rs.getTimestamp(3));
            g.setStatsLeft(rs.getLong(4));
            g.setLost(rs.getLong(5));
            g.setFatalError(rs.getString(6));
            result.add(g);
        }
        return result;
    }
}
