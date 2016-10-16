package com.github.endoscope.storage.jdbc;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QueryRunnerExt extends QueryRunner {
    private int fetchSize = 0;

    public QueryRunnerExt() {
    }

    public QueryRunnerExt(boolean pmdKnownBroken) {
        super(pmdKnownBroken);
    }

    public QueryRunnerExt(DataSource ds) {
        super(ds);
    }

    public QueryRunnerExt(DataSource ds, boolean pmdKnownBroken) {
        super(ds, pmdKnownBroken);
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    private void applyFetchSize(PreparedStatement ps) throws SQLException {
        if( fetchSize > 0 ){
            ps.setFetchSize(fetchSize);
        }
    }

    @Override
    protected PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        PreparedStatement ps = super.prepareStatement(conn, sql);
        applyFetchSize(ps);
        return ps;
    }

    @Override
    protected PreparedStatement prepareStatement(Connection conn, String sql, int returnedKeys) throws SQLException {
        PreparedStatement ps = super.prepareStatement(conn, sql, returnedKeys);
        applyFetchSize(ps);
        return ps;
    }

    public <T> T queryExt(int fetchSize, String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
        int previous = this.fetchSize;
        this.fetchSize = fetchSize;
        try {
            return query(sql, rsh, params);
        }finally{
            this.fetchSize = previous;
        }
    }
}
