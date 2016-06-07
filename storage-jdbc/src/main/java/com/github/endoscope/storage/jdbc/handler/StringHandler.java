package com.github.endoscope.storage.jdbc.handler;

import org.apache.commons.dbutils.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StringHandler implements ResultSetHandler<List<String>> {
    public List<String> handle(ResultSet rs) throws SQLException {
        List<String> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getString(1));
        }
        return result;
    }
}
