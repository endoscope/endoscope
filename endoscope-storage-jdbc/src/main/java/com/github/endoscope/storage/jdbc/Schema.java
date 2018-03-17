package com.github.endoscope.storage.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;

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
                            "  err INT, " +
                            "  max INT, " +
                            "  min INT, " +
                            "  avg INT, " +
                            "  hasChildren INT " +
                            ")");

            run.update(
                    "CREATE TABLE IF NOT EXISTS endoscopeDailyStat(" +
                            "  id VARCHAR(36) PRIMARY KEY, " +
                            "  groupId VARCHAR(36), " +
                            "  parentId VARCHAR(36), " +
                            "  rootId VARCHAR(36), " +
                            "  name VARCHAR(255), " +
                            "  hits INT, " +
                            "  err INT, " +
                            "  max INT, " +
                            "  min INT, " +
                            "  avg INT, " +
                            "  hasChildren INT " +
                            ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createH2TablesClob(DataSource ds) {
        QueryRunner run = new QueryRunner(ds);

        //this is DB specific - so far just for H2
        try {
            run.update(
                    "CREATE TABLE IF NOT EXISTS clob_endoscopeGroup(" +
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
                    "CREATE TABLE IF NOT EXISTS clob_endoscopeStat(" +
                            "  id VARCHAR(36) PRIMARY KEY, " +
                            "  groupId VARCHAR(36), " +
                            "  rootId VARCHAR(36), " +
                            "  name VARCHAR(255), " +
                            "  hits INT, " +
                            "  err INT, " +
                            "  max INT, " +
                            "  min INT, " +
                            "  avg INT, " +
                            "  hasChildren INT, " +
                            "  children CLOB " +
                            ")");

            run.update(
                    "CREATE TABLE IF NOT EXISTS clob_endoscopeDailyStat(" +
                            "  id VARCHAR(36) PRIMARY KEY, " +
                            "  groupId VARCHAR(36), " +
                            "  rootId VARCHAR(36), " +
                            "  name VARCHAR(255), " +
                            "  hits INT, " +
                            "  err INT, " +
                            "  max INT, " +
                            "  min INT, " +
                            "  avg INT, " +
                            "  hasChildren INT " +
                            ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
