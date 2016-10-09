package com.github.endoscope.storage.jdbc;

import com.github.storage.test.StorageTestCases;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.AfterClass;

import javax.sql.DataSource;

public class JdbcStorageTest extends StorageTestCases {
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

    public JdbcStorageTest(){
        super(new JdbcStorage2());

        try{
            server = Server.createTcpServer().start();
            server.start();

            ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");
            Schema.createH2Tables(ds);

            storage.setup(TestDsProvider.class.getName());
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
