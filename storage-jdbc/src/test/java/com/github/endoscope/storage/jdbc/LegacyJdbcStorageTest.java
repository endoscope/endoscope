package com.github.endoscope.storage.jdbc;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.properties.Properties;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.StatHistory;
import com.github.endoscope.util.JsonUtil;
import com.github.endoscope.util.PropertyTestUtil;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class LegacyJdbcStorageTest {
    private static Server server;
    private static JdbcDataSource ds;

    private static Context contextMock = Mockito.mock(Context.class);

    public static final class NCF implements InitialContextFactory {
        public Context getInitialContext(Hashtable<?,?> environment) throws NamingException {
            return contextMock;
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        server = Server.createTcpServer().start();
        server.start();

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, NCF.class.getName());

        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");

        Schema.createH2Tables(ds);

        Mockito.when(contextMock.lookup("jdbc/dsName")).thenReturn(ds);
    }

    @AfterClass
    public static void finish(){
        server.stop();
    }

    private class DateFactory {
        long base;

        public DateFactory(long base){
            this.base = base;
        }

        public Date date(long d){
            return new Date(base + d);
        }
    }

    @Test
    public void should_save_and_read_top_level_stats() throws SQLException {
        DateFactory df = new DateFactory(0);

        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        Stats stats = buildStats(df.date(1000L), df.date(2000L), 123);
        storage.save(stats);

        Stats read = storage.topLevel(df.date(1000L), df.date(2000L), null, null);
        assertNotNull(read);
        assertEquals(stats.getStatsLeft(), read.getStatsLeft());
        assertEquals(stats.getLost(), read.getLost());
        assertEquals(stats.getFatalError(), read.getFatalError());
        assertEquals(stats.getStartDate(), read.getStartDate());
        assertEquals(stats.getEndDate(), read.getEndDate());

        assertEquals(stats.getMap().size(), read.getMap().size());
        assertEquals(stats.getMap().get("s1").getHits(), read.getMap().get("s1").getHits());
        assertEquals(stats.getMap().get("s1").getMax(), read.getMap().get("s1").getMax());
        assertNull(read.getMap().get("s2").getChildren());
        assertEquals(0, read.getMap().get("s1").getChildren().size());
    }

    @Test
    public void should_save_and_read_detail_stats() throws SQLException {
        DateFactory df = new DateFactory(20000);

        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        Stats stats = buildStats(df.date(1000), df.date(2000), 27, 123);
        storage.save(stats);

        StatDetails read = storage.stat("s1", df.date(1000), df.date(2000), null, null);

        dumpDB();
        //dumpObjects(stats.getMap().get("s1"), read);

        assertNotNull(read);
        assertEquals("s1", read.getId());
        assertEquals(27, read.getMerged().getMin());
        assertEquals(123, read.getMerged().getMax());
        assertEquals(75, read.getMerged().getAvg());
        assertEquals(2, read.getMerged().getHits());
        assertEquals(1, read.getMerged().getChildren().size());
        assertEquals(270, read.getMerged().getChildren().get("s11").getMin());
        assertEquals(1230, read.getMerged().getChildren().get("s11").getMax());
        assertEquals(750, read.getMerged().getChildren().get("s11").getAvg());
        assertEquals(2, read.getMerged().getChildren().get("s11").getHits());
        assertNull(read.getMerged().getChildren().get("s11").getChildren());

        read = storage.stat("s2", df.date(1000), df.date(2000), null, null);
        assertNotNull(read);
        assertEquals("s2", read.getId());
        assertEquals(2*123, read.getMerged().getMax());
    }

    private void dumpObjects(Object ... oo) {
        JsonUtil jsonUtil = new JsonUtil(true);
        for( Object o : oo ){
            System.out.println(jsonUtil.toJson(o));
        }
    }

    @Test
    public void should_find_stats_in_given_range() throws SQLException {
        DateFactory df = new DateFactory(10000);

        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        storage.save(buildStats(df.date(1000), df.date(2000), 123));
        storage.save(buildStats(df.date(3000), df.date(4000), 456));
        storage.save(buildStats(df.date(5000), df.date(6000), 789));

        //first are in range
        assertEquals(123, storage.topLevel(df.date(0), df.date(3500), null, null).getMap().get("s1").getMax());
        assertEquals(123, storage.stat("s1", df.date(0), df.date(3500), null, null).getMerged().getMax());

        //first exactly in range
        assertEquals(123, storage.topLevel(df.date(1000), df.date(2000), null, null).getMap().get("s1").getMax());
        assertEquals(123, storage.stat("s1", df.date(1000L), df.date(2000), null, null).getMerged().getMax());

        //second in range
        assertEquals(456, storage.topLevel(df.date(1500), df.date(5500), null, null).getMap().get("s1").getMax());
        assertEquals(456, storage.stat("s1", df.date(1500), df.date(5500), null, null).getMerged().getMax());

        //second exactly in range
        assertEquals(456, storage.topLevel(df.date(3000), df.date(4000), null, null).getMap().get("s1").getMax());
        assertEquals(456, storage.stat("s1", df.date(3000), df.date(4000), null, null).getMerged().getMax());

        //third in range
        assertEquals(789, storage.topLevel(df.date(3500), df.date(7500), null, null).getMap().get("s1").getMax());
        assertEquals(789, storage.stat("s1", df.date(3500), df.date(7500), null, null).getMerged().getMax());

        //third exactly in range
        assertEquals(789, storage.topLevel(df.date(5000), df.date(6000), null, null).getMap().get("s1").getMax());
        assertEquals(789, storage.stat("s1", df.date(5000), df.date(6000), null, null).getMerged().getMax());
    }

    @Test
    public void should_read_histogram() throws SQLException {
        DateFactory df = new DateFactory(30000);

        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        storage.save(buildStats(df.date(1000), df.date(2000), 1));
        storage.save(buildStats(df.date(3000), df.date(4000), 2, 20));
        storage.save(buildStats(df.date(5000), df.date(6000), 3, 30, 300));

        dumpDB();
        StatDetails details = storage.stat("s1", df.date(1000), df.date(9000), null, null);
        List<StatHistory> history = details.getHistogram();

        assertEquals(3, history.size());
        assertEquals(df.date(1000), history.get(0).getStartDate());
        assertEquals(df.date(2000), history.get(0).getEndDate());
        assertEquals(1, history.get(0).getHits());
        assertEquals(1, history.get(0).getMin());
        assertEquals(1, history.get(0).getMax());
        assertEquals(1, history.get(0).getAvg());

        assertEquals(df.date(3000), history.get(1).getStartDate());
        assertEquals(df.date(4000), history.get(1).getEndDate());
        assertEquals(2, history.get(1).getHits());
        assertEquals(2, history.get(1).getMin());
        assertEquals(20, history.get(1).getMax());
        assertEquals(11, history.get(1).getAvg());

        assertEquals(df.date(5000), history.get(2).getStartDate());
        assertEquals(df.date(6000), history.get(2).getEndDate());
        assertEquals(3, history.get(2).getHits());
        assertEquals(3, history.get(2).getMin());
        assertEquals(300, history.get(2).getMax());
        assertEquals(111, history.get(2).getAvg());
    }

    private static void dumpDB(){
        try {
            q("SELECT * from endoscopeGroup");
            q("SELECT * from endoscopestat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void q(String q) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        List<Map<String, Object>> result = run.query(q, new MapListHandler());
        System.out.println(result);
    }

    private Stats buildStats(Date from, Date to, long ... time) {
        Stats stats = new Stats();
        stats.setFatalError("error");
        stats.setStartDate(from);
        stats.setEndDate(to);
        stats.setLost(111);

        Stat s1 = buildStat(1, time);
        Stat s11 = buildStat(10, time);

        stats.getMap().put("s1", s1);
        s1.ensureChildrenMap();
        s1.getChildren().put("s11", s11);

        stats.getMap().put("s2", buildStat(2, time));

        return stats;
    }

    private Stat buildStat(long multiplier, long ... time){
        Stat stat = new Stat();
        stat.setMin(Long.MAX_VALUE);
        for( long t : time ){
            stat.update(multiplier * t);
        }
        return stat;
    }

    @Test
    public void should_read_filters() throws SQLException {
        DateFactory df = new DateFactory(40000);

        PropertyTestUtil.withProperty(Properties.APP_INSTANCE, "g1", ()->{
            PropertyTestUtil.withProperty(Properties.APP_TYPE,  "t9", ()->{
                SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
                storage.save(buildStats(df.date(1000), df.date(2000), 1));
            });
        });

        PropertyTestUtil.withProperty(Properties.APP_INSTANCE, "g2", ()->{
            PropertyTestUtil.withProperty(Properties.APP_TYPE,  "t8", ()->{
                SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
                storage.save(buildStats(df.date(3000), df.date(4000), 2, 20));
            });
        });
        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");

        Filters filters = storage.filters(df.date(0), df.date(9000));
        assertArrayEquals(new Object[]{"g1", "g2"}, filters.getInstances().toArray());
        assertArrayEquals(new Object[]{"t8", "t9"}, filters.getTypes().toArray());

        filters = storage.filters(df.date(0), df.date(2500));
        assertArrayEquals(new Object[]{"g1"}, filters.getInstances().toArray());
        assertArrayEquals(new Object[]{"t9"}, filters.getTypes().toArray());

        filters = storage.filters(df.date(2500), df.date(9000));
        assertArrayEquals(new Object[]{"g2"}, filters.getInstances().toArray());
        assertArrayEquals(new Object[]{"t8"}, filters.getTypes().toArray());
    }
}
