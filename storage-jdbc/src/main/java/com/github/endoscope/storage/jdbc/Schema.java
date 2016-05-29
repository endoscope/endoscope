package com.github.endoscope.storage.jdbc;

import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Schema {
    public static void createH2Tables(DataSource ds) {
        QueryRunner run = new QueryRunner(ds);

        //this is DB specific - so far just for H2
        try {
            run.update(
                    "CREATE TABLE IF NOT EXISTS endoscopeGroup(" +
                            "  id VARCHAR(36) PRIMARY KEY, " +
                            "  startDate TIMESTAMP, " +
                            "  endDate TIMESTAMP, " +
                            "  statsLeft INT, " +
                            "  lost INT, " +
                            "  fatalError VARCHAR(255), " +
                            "  appGroup VARCHAR(100), " +
                            "  appType VARCHAR(100)" +
                            ")");

            run.update(
                    "CREATE TABLE IF NOT EXISTS endoscopeStat(" +
                            "  id VARCHAR(36) PRIMARY KEY, " +
                            "  groupId VARCHAR(36), " +
                            "  parentId VARCHAR(36), " +
                            "  rootId VARCHAR(36), " +
                            "  name VARCHAR(255), " +
                            "  hits INT, " +
                            "  max INT, " +
                            "  min INT, " +
                            "  avg INT, " +
                            "  ah10 INT, " +
                            "  hasChildren INT " +
                            ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
