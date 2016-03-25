package org.endoscope.storage.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ListOfMapRSHandler implements ResultSetHandler<List<Map<String, Object>>> {
    public List<Map<String, Object>> handle(ResultSet rs) throws SQLException {
        int columns = rs.getMetaData().getColumnCount();
        List result = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> record = new HashMap<>();
            for(int c=1; c<=columns; c++){
                record.put(rs.getMetaData().getColumnName(c),rs.getObject(c));
            }
            result.add(record);
        }
        return result;
    }
}
