package com.github.storage.test;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.github.endoscope.util.DateUtil.parseDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class StorageTestCases {
    private static final String STAT_NAME = "the-stat";
    protected Storage storage;
    protected JsonUtil jsonUtil = new JsonUtil();
    private static int year = 2000;

    public StorageTestCases(Storage storage){
        this.storage = storage;
    }

    private Stat stat(long ... times){
        Stat stat = new Stat();
        for( long time : times ){
            stat.update(time);
        }
        return stat;
    }

    protected Stats stats(Date from, Date to, Stat stat) {
        Stats stats = new Stats();
        stats.setFatalError("error");
        stats.setStartDate(from);
        stats.setEndDate(to);
        stats.setLost(111);
        stats.getMap().put(STAT_NAME, stat);
        return stats;
    }

    protected Stats stats(Date from, Date to) {
        return stats(from, to, stat(100));
    }

    private Date dt(String date){
        return parseDateTime(date);
    }

    @Before
    public void before(){
        //stats share the same storage so we must ensure that each test works on it's own
        year++;
    }

    @Test
    public void should_save_and_load_single_stats() throws IOException{
        Stats stats = stats( dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00"));
        String identifier = storage.save(stats, null, null);
        Stats loaded = storage.load(identifier);

        assertEquals(stats, loaded);
        assertEquals(jsonUtil.toJson(stats), jsonUtil.toJson(loaded));
    }

    @Test
    public void should_find_one_stats_in_exact_range() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, null);
        String id2 = storage.save(stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00")), null, null);
        String id3 = storage.save(stats(dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00")), null, null);

        List<String> ids = storage.find(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00"), null, null);

        assertEquals(1, ids.size() );
        assertEquals(id2, ids.get(0) );
    }

    @Test
    public void should_find_one_stats_in_bigger_range() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, null);
        String id2 = storage.save(stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00")), null, null);
        String id3 = storage.save(stats(dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00")), null, null);

        List<String> ids = storage.find(dt(year+"-01-01 08:10:00"), dt(year+"-01-01 08:40:00"), null, null);

        assertEquals(1, ids.size() );
        assertEquals(id2, ids.get(0) );
    }

    @Test
    public void should_find_two_stats_in_bigger_range() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, null);
        String id2 = storage.save(stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00")), null, null);
        String id3 = storage.save(stats(dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00")), null, null);

        List<String> ids = storage.find(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), null, null);

        assertEquals(2, ids.size() );
        assertEquals(id1, ids.get(0) );
        assertEquals(id2, ids.get(1) );
    }

    @Test
    public void should_find_one_stats_matching_instance() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", null);
        String id2 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i2", null);
        String id3 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i3", null);

        List<String> ids = storage.find(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), "i2", null);

        assertEquals(1, ids.size() );
        assertEquals(id2, ids.get(0) );
    }

    @Test
    public void should_find_one_stats_matching_type() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, "t1");
        String id2 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, "t2");
        String id3 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), null, "t3");

        List<String> ids = storage.find(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), null, "t2");

        assertEquals(1, ids.size() );
        assertEquals(id2, ids.get(0) );
    }

    @Test
    public void should_find_one_stats_matching_instance_and_type() throws IOException{
        String id1 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t1");
        String id2 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i2", "t1");
        String id3 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t2");
        String id4 = storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i2", "t2");

        List<String> ids = storage.find(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), "i2", "t1");

        assertEquals(1, ids.size() );
        assertEquals(id2, ids.get(0) );
    }

    @Test
    public void should_find_all_filters() throws IOException{
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t1");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i2", "t1");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t2");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i3", "t2");

        Filters filters = storage.findFilters(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), null);

        assertEquals( 3, filters.getInstances().size() );
        assertTrue(filters.getInstances().contains("i1"));
        assertTrue(filters.getInstances().contains("i2"));
        assertTrue(filters.getInstances().contains("i3"));

        assertEquals( 2, filters.getTypes().size() );
        assertTrue(filters.getTypes().contains("t1"));
        assertTrue(filters.getTypes().contains("t2"));
    }

    @Test
    public void should_find_filters_for_date_range() throws IOException{
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t1");
        storage.save(stats(dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:40:00")), "i2", "t1");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t2");
        storage.save(stats(dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:40:00")), "i3", "t2");

        Filters filters = storage.findFilters(dt(year+"-01-01 08:20:00"), dt(year+"-01-01 08:50:00"), null);

        assertEquals( 2, filters.getInstances().size() );
        assertTrue(filters.getInstances().contains("i2"));
        assertTrue(filters.getInstances().contains("i3"));

        assertEquals( 2, filters.getTypes().size() );
        assertTrue(filters.getTypes().contains("t1"));
        assertTrue(filters.getTypes().contains("t2"));
    }

    @Test
    public void should_find_filters_for_type() throws IOException{
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t1");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i2", "t1");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i1", "t2");
        storage.save(stats(dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:15:00")), "i3", "t2");

        Filters filters = storage.findFilters(dt(year+"-01-01 07:10:00"), dt(year+"-01-01 08:40:00"), "t1");

        assertEquals( 2, filters.getInstances().size() );
        assertTrue(filters.getInstances().contains("i1"));
        assertTrue(filters.getInstances().contains("i2"));

        assertEquals( 1, filters.getTypes().size() );
        assertTrue(filters.getTypes().contains("t1"));
    }

    @Test
    public void should_load_details_by_stats_ids() throws IOException{
        //given
        Stats stats1 = stats(
                dt(year+"-01-01 08:00:00"),
                dt(year+"-01-01 08:15:00"),
                stat(100, 200)//hits = 2, avg = 150
        );

        Stats stats2 = stats(
                dt(year+"-01-01 08:15:00"),
                dt(year+"-01-01 08:30:00"),
                stat(1000)//hits = 1, avg = 1000
        );

        String id1 = storage.save(stats1, null, null);
        String id2 = storage.save(stats2, null, null);

        //when
        StatDetails details = storage.loadDetails(STAT_NAME, Arrays.asList(id1, id2));

        //then
        Stat expected = stats1.getMap().get(STAT_NAME).deepCopy();
        expected.merge(stats2.getMap().get(STAT_NAME));

        assertEquals(STAT_NAME, details.getId() );
        assertEquals( expected, details.getMerged());
        assertEquals( 2, details.getHistogram().size());

        assertEquals( 150L, details.getHistogram().get(0).getAvg() );
        assertEquals( dt(year+"-01-01 08:00:00"), details.getHistogram().get(0).getStartDate() );
        assertEquals( dt(year+"-01-01 08:15:00"), details.getHistogram().get(0).getEndDate() );
        assertEquals( 2, details.getHistogram().get(0).getHits() );

        assertEquals( 1000L, details.getHistogram().get(1).getAvg() );
        assertEquals( dt(year+"-01-01 08:15:00"), details.getHistogram().get(1).getStartDate() );
        assertEquals( dt(year+"-01-01 08:30:00"), details.getHistogram().get(1).getEndDate() );
        assertEquals( 1, details.getHistogram().get(1).getHits() );
    }

    @Test
    public void should_load_details_by_date_range() throws IOException{
        //given
        Stats stats1 = stats(
                dt(year+"-01-01 08:00:00"),
                dt(year+"-01-01 08:15:00"),
                stat(100, 200)//hits = 2, avg = 150
        );
        Stats stats2 = stats(
                dt(year+"-01-01 08:15:00"),
                dt(year+"-01-01 08:30:00"),
                stat(1000) //hits =1 avg = 1000
        );
        Stats stats3 = stats(
                dt(year+"-01-01 08:30:00"),
                dt(year+"-01-01 08:45:00"),
                stat(1000, 1000, 1000)// hits = 3, avg = 1000
        );

        storage.save(stats1, null, null);
        storage.save(stats2, null, null);
        storage.save(stats3, null, null);

        //when
        StatDetails details = storage.loadDetails(STAT_NAME, dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00"), null, null);

        //then
        assertEquals(STAT_NAME, details.getId() );
        assertEquals( 1, details.getMerged().getHits());
        assertEquals( 1, details.getHistogram().size());
        assertEquals( 1000L, details.getHistogram().get(0).getAvg() );
        assertEquals( dt(year+"-01-01 08:15:00"), details.getHistogram().get(0).getStartDate() );
        assertEquals( dt(year+"-01-01 08:30:00"), details.getHistogram().get(0).getEndDate() );
        assertEquals( 1, details.getHistogram().get(0).getHits() );
    }

    @Test
    public void should_load_details_by_instance_and_type() throws IOException{
        //given
        Stats stats = stats(dt(year+"-01-01 08:05:00"), dt(year+"-01-01 08:15:00"), stat(100));

        //save the same but with different instance and type
        storage.save(stats, "i1", null);
        storage.save(stats, "i1", "t1");
        storage.save(stats, "i2", "t1");

        //when
        StatDetails detailsI  = storage.loadDetails(STAT_NAME, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:30:00"), "i1", null);
        StatDetails detailsT  = storage.loadDetails(STAT_NAME, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:30:00"), null, "t1");
        StatDetails detailsIT = storage.loadDetails(STAT_NAME, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:30:00"), "i1", "t1");

        //then
        assertEquals( 2, detailsI.getHistogram().size());
        assertEquals( 2, detailsT.getHistogram().size());
        assertEquals( 1, detailsIT.getHistogram().size());
    }

    @Test
    public void should_load_top_level() throws IOException{
        //given
        Stat parentStat = stat(100);
        Stat childStat = stat(10);
        parentStat.ensureChildrenMap();
        parentStat.getChildren().put("child", childStat);

        Stats stats = stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:30:00"), parentStat);

        storage.save(stats, null, null);

        //when
        Stats topLevel = storage.loadAggregated(true, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:45:00"), null, null);

        //then
        Stat root = topLevel.getMap().get(STAT_NAME);
        assertNotNull(root);
        assertNotNull( root.getChildren() ); //not null map means that root has children ...
        assertEquals( 0, root.getChildren().size());///but for top level only children are empty
    }

    @Test
    public void should_load_aggregated() throws IOException{
        //given
        Stat parentStat = stat(100);
        Stat childStat = stat(10);
        parentStat.ensureChildrenMap();
        parentStat.getChildren().put("child", childStat);

        Stats stats1 = stats(dt(year+"-01-01 08:05:00"), dt(year+"-01-01 08:10:00"), parentStat);
        stats1.setLost(10);
        stats1.setFatalError("e1");
        storage.save(stats1, null, null);

        Stats stats2 = stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:20:00"), parentStat);
        stats2.setLost(100);
        stats2.setFatalError("e2");
        storage.save(stats2, null, null);

        //when
        Stats loaded = storage.loadAggregated(false, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:30:00"), null, null);

        //then
        assertEquals( dt(year+"-01-01 08:05:00"), loaded.getStartDate());
        assertEquals( dt(year+"-01-01 08:20:00"), loaded.getEndDate());

        assertEquals( 110, loaded.getLost());
        assertTrue( loaded.getFatalError().startsWith("e"));

        assertEquals( 1, loaded.getMap().size());

        Stat loadedChild = loaded.getMap().get(STAT_NAME).getChild("child");

        assertEquals( 10L, loadedChild.getAvg());
        assertEquals( 2, loadedChild.getHits());
    }

    @Test
    public void should_load_aggregated_top_level() throws IOException{
        //given
        Stat parentStat = stat(100);
        Stat childStat = stat(10);
        parentStat.ensureChildrenMap();
        parentStat.getChildren().put("child", childStat);

        Stats stats1 = stats(dt(year+"-01-01 08:05:00"), dt(year+"-01-01 08:10:00"), parentStat);
        storage.save(stats1, null, null);

        Stats stats2 = stats(dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:20:00"), parentStat);
        storage.save(stats2, null, null);

        //when
        Stats loaded = storage.loadAggregated(true, dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:30:00"), null, null);

        //then
        assertEquals( dt(year+"-01-01 08:05:00"), loaded.getStartDate());
        assertEquals( dt(year+"-01-01 08:20:00"), loaded.getEndDate());

        assertEquals( 1, loaded.getMap().size());

        Stat loadedRoot = loaded.getMap().get(STAT_NAME);

        assertEquals( 100L, loadedRoot.getAvg());
        assertEquals( 2, loadedRoot.getHits());
        assertNotNull( loadedRoot.getChildren());
        assertEquals( 0, loadedRoot.getChildren().size());
    }

    @Test
    public void should_load_aggregated_by_instance() throws IOException{
        //given
        storage.save(stats( dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:10:00"), stat(10)), "i1", null);
        storage.save(stats( dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:20:00"), stat(100)), "i1", "t1");
        storage.save(stats( dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00"), stat(1000)), "i2", "t1");

        //when
        Stats aggrI  = storage.loadAggregated(false, dt(year+"-01-01 07:00:00"), dt(year+"-01-01 09:30:00"), "i1", null);

        //then
        Stats aggregated = aggrI;
        Stat stat = aggregated.getMap().get(STAT_NAME);
        assertEquals( dt(year+"-01-01 08:00:00"), aggregated.getStartDate());
        assertEquals( dt(year+"-01-01 08:20:00"), aggregated.getEndDate());
        assertEquals( 1, aggregated.getMap().size());
        assertEquals( 55L, stat.getAvg());
        assertEquals( 2L, stat.getHits());
        assertEquals( 100L, stat.getMax());
        assertEquals( 10L, stat.getMin());
    }

    @Test
    public void should_load_aggregated_by_type() throws IOException{
        //given
        storage.save(stats( dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:10:00"), stat(10)), "i1", null);
        storage.save(stats( dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:20:00"), stat(100)), "i1", "t1");
        storage.save(stats( dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00"), stat(1000)), "i2", "t1");

        //when
        Stats aggrT  = storage.loadAggregated(false, dt(year+"-01-01 07:00:00"), dt(year+"-01-01 09:30:00"), null, "t1");

        //then
        Stats aggregated = aggrT;
        Stat stat = aggregated.getMap().get(STAT_NAME);
        assertEquals( dt(year+"-01-01 08:15:00"), aggregated.getStartDate());
        assertEquals( dt(year+"-01-01 08:45:00"), aggregated.getEndDate());
        assertEquals( 1, aggregated.getMap().size());
        assertEquals( 550L, stat.getAvg());
        assertEquals( 2L, stat.getHits());
        assertEquals( 1000L, stat.getMax());
        assertEquals( 100L, stat.getMin());
    }

    @Test
    public void should_load_aggregated_by_instance_and_type() throws IOException{
        //given
        storage.save(stats( dt(year+"-01-01 08:00:00"), dt(year+"-01-01 08:10:00"), stat(10)), "i1", null);
        storage.save(stats( dt(year+"-01-01 08:15:00"), dt(year+"-01-01 08:20:00"), stat(100)), "i1", "t1");
        storage.save(stats( dt(year+"-01-01 08:30:00"), dt(year+"-01-01 08:45:00"), stat(1000)), "i2", "t1");

        //when
        Stats aggrIT = storage.loadAggregated(false, dt(year+"-01-01 07:00:00"), dt(year+"-01-01 09:30:00"), "i1", "t1");

        //then
        Stats aggregated = aggrIT;
        Stat stat = aggregated.getMap().get(STAT_NAME);
        assertEquals( dt(year+"-01-01 08:15:00"), aggregated.getStartDate());
        assertEquals( dt(year+"-01-01 08:20:00"), aggregated.getEndDate());
        assertEquals( 1, aggregated.getMap().size());
        assertEquals( 100L, stat.getAvg());
        assertEquals( 1L, stat.getHits());
        assertEquals( 100L, stat.getMax());
        assertEquals( 100L, stat.getMin());
    }
}
