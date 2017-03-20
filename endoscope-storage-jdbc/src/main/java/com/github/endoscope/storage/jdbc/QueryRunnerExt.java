package com.github.endoscope.storage.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class QueryRunnerExt extends QueryRunner {
    private ThreadLocal<Integer> fetchSize = ThreadLocal.withInitial(() -> null);

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
        return fetchSize.get();
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize.set(fetchSize);
    }

    private void applyFetchSize(PreparedStatement ps) throws SQLException {
        if( fetchSize.get() != null ){
            ps.setFetchSize(fetchSize.get());
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
        Integer previous = this.fetchSize.get();
        this.fetchSize.set(fetchSize);
        try {
            return query(sql, rsh, params);
        }finally{
            this.fetchSize.set(previous);
        }
    }
}
