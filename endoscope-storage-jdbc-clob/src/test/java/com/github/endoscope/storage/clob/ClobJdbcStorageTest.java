package com.github.endoscope.storage.clob;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.jdbc.DataSourceProvider;
import com.github.endoscope.storage.jdbc.Schema;
import com.github.storage.test.StorageTestCases;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClobJdbcStorageTest extends StorageTestCases {
    private static Server server;
    private static JdbcDataSource ds;

    @AfterClass
    public static void finish(){
        server.stop();
    }

    public static class TestDsProvider implements DataSourceProvider {
        @Override
        public DataSource create(String initParam) {
            return ds;
        }
    }

    public ClobJdbcStorageTest(){
        super(new ClobJdbcStorage());

        try{
            server = Server.createTcpServer().start();
            server.start();

            ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");
            Schema.createH2TablesClob(ds);

            storage.setup(TestDsProvider.class.getName());
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Test
    public void should_cleanup_old_stats() throws IOException {
        //warning fixed dates may destabilize tests if data overlaps
        Stats oldStats = stats( dt("1990-01-01 08:00:00"), dt("1990-01-01 08:15:00"));
        String oldIdentifier = storage.save(oldStats, null, "cleanup-instance-test");

        Stats newStats = stats( dt(getYear()+"-01-01 08:00:00"), dt(getYear()+"-01-01 08:15:00"));
        String newIdentifier = storage.save(newStats, null, "cleanup-instance-test");

        List<String> ids = storage.find(dt("1980-01-01 08:00:00"), dt(getYear()+"-12-01 08:00:00"), null, "cleanup-instance-test");
        assertEquals(2, ids.size() );

        int oneDay = 1;
        storage.cleanup(oneDay, "cleanup-instance-test");

        ids = storage.find(dt("1980-01-01 08:00:00"), dt(getYear()+"-12-01 08:00:00"), null, "cleanup-instance-test");
        assertEquals(1, ids.size() );
        assertEquals(newIdentifier, ids.get(0) );
    }
}
