package org.endoscope.storage.jdbc;

import org.apache.commons.dbutils.QueryRunner;
import org.endoscope.core.Stat;
import org.endoscope.core.Stats;
import org.endoscope.storage.StatDetails;
import org.endoscope.storage.StatHistory;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JdbcStorageTest {
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

        Mockito.when(contextMock.lookup("jdbc/dsName")).thenReturn(ds);
    }

    @AfterClass
    public static void finish(){
        server.stop();
    }

    @Test
    public void should_save_and_read_top_level_stats() throws SQLException {
        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        Stats stats = buildStats(new Date(1000L), new Date(2000L), 123);
        storage.save(stats);

        Stats read = storage.topLevel(new Date(1000L), new Date(2000L));
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
        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        Stats stats = buildStats(new Date(21000L), new Date(22000L), 27, 123);
        storage.save(stats);

        dump();

        StatDetails read = storage.stat("s1", new Date(21000L), new Date(22000L));
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

        read = storage.stat("s2", new Date(21000L), new Date(22000L));
        assertNotNull(read);
        assertEquals("s2", read.getId());
        assertEquals(2*123, read.getMerged().getMax());
    }

    @Test
    public void should_find_stats_in_given_range() throws SQLException {
        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        storage.save(buildStats(new Date(11000L), new Date(12000L), 123));
        storage.save(buildStats(new Date(13000L), new Date(14000L), 456));
        storage.save(buildStats(new Date(15000L), new Date(16000L), 789));

        //first are in range
        assertEquals(123, storage.topLevel(new Date(10000L), new Date(13500L)).getMap().get("s1").getMax());
        assertEquals(123, storage.stat("s1", new Date(10000L), new Date(13500L)).getMerged().getMax());

        //first exactly in range
        assertEquals(123, storage.topLevel(new Date(11000L), new Date(12000L)).getMap().get("s1").getMax());
        assertEquals(123, storage.stat("s1", new Date(11000L), new Date(12000L)).getMerged().getMax());

        //second in range
        assertEquals(456, storage.topLevel(new Date(11500L), new Date(15500L)).getMap().get("s1").getMax());
        assertEquals(456, storage.stat("s1", new Date(11500L), new Date(15500L)).getMerged().getMax());

        //second exactly in range
        assertEquals(456, storage.topLevel(new Date(13000L), new Date(14000L)).getMap().get("s1").getMax());
        assertEquals(456, storage.stat("s1", new Date(13000L), new Date(14000L)).getMerged().getMax());

        //third in range
        assertEquals(789, storage.topLevel(new Date(13500L), new Date(17500L)).getMap().get("s1").getMax());
        assertEquals(789, storage.stat("s1", new Date(13500L), new Date(17500L)).getMerged().getMax());

        //third exactly in range
        assertEquals(789, storage.topLevel(new Date(15000L), new Date(16000L)).getMap().get("s1").getMax());
        assertEquals(789, storage.stat("s1", new Date(15000L), new Date(16000L)).getMerged().getMax());
    }

    @Test
    public void should_read_histogram() throws SQLException {
        SearchableJdbcStorage storage = new SearchableJdbcStorage("jdbc/dsName");
        storage.save(buildStats(new Date(311000L), new Date(312000L), 1));
        storage.save(buildStats(new Date(313000L), new Date(314000L), 2, 20));
        storage.save(buildStats(new Date(315000L), new Date(316000L), 3, 30, 300));

        StatDetails details = storage.stat("s1", new Date(311000L), new Date(333316000L));
        List<StatHistory> history = details.getHistogram();

        assertEquals(3, history.size());
        assertEquals(new Date(311000L), history.get(0).getStartDate());
        assertEquals(new Date(312000L), history.get(0).getEndDate());
        assertEquals(1, history.get(0).getHits());
        assertEquals(1, history.get(0).getMin());
        assertEquals(1, history.get(0).getMax());
        assertEquals(1, history.get(0).getAvg());

        assertEquals(new Date(313000L), history.get(1).getStartDate());
        assertEquals(new Date(314000L), history.get(1).getEndDate());
        assertEquals(2, history.get(1).getHits());
        assertEquals(2, history.get(1).getMin());
        assertEquals(20, history.get(1).getMax());
        assertEquals(11, history.get(1).getAvg());

        assertEquals(new Date(315000L), history.get(2).getStartDate());
        assertEquals(new Date(316000L), history.get(2).getEndDate());
        assertEquals(3, history.get(2).getHits());
        assertEquals(3, history.get(2).getMin());
        assertEquals(300, history.get(2).getMax());
        assertEquals(111, history.get(2).getAvg());
    }

    private static void dump(){
        try {
            q("SELECT * from endoscopeGroup");
            q("SELECT * from endoscopestat");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void q(String q) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        List<Map<String, Object>> result = run.query(q, new ListOfMapRSHandler());
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
}
