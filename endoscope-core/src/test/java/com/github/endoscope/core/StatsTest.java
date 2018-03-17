package com.github.endoscope.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.github.endoscope.properties.Properties;
import com.github.endoscope.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import static com.github.endoscope.util.DateUtil.parseDateTime;
import static com.github.endoscope.util.PropertyTestUtil.withProperty;
import static org.junit.Assert.assertEquals;

public class StatsTest {
    final JsonUtil jsonUtil = new JsonUtil(true);

    private <T> T fromResourceJson(String resourceName, Class<T> clazz){
        InputStream src = this.getClass().getResourceAsStream(resourceName);
        try{
            return jsonUtil.fromJson(clazz, src);
        }finally{
            IOUtils.closeQuietly(src);
        }
    }

    private String getResourceString(String resourceName){
        InputStream src = this.getClass().getResourceAsStream(resourceName);
        try {
            return IOUtils.toString(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally{
            IOUtils.closeQuietly(src);
        }
    }

    private void process(String input, String output) {
        Context context = fromResourceJson(input, Context.class);

        Stats stats = new Stats();
        stats.store(context);

        String result = jsonUtil.toJson(stats.getMap());

        String expected = getResourceString(output);
        assertEquals(expected, result);
    }

    @Test
    public void should_collect_stats_1(){
        process("/input1.json", "/expected1.json");
    }

    @Test
    public void should_collect_stats_2(){
        process("/input2.json", "/expected2.json");
    }

    @Test
    public void should_collect_stats_3(){
        process("/input3.json", "/expected3.json");
    }

    @Test
    public void should_limit_number_of_stats(){
        //stats over limit will be ignored
        withProperty(Properties.MAX_STAT_COUNT, "2", ()->{
            process("/input4.json", "/expected4.json");
        });
    }

    @Test
    public void should_increment_lost(){
        Stats s = new Stats();
        assertEquals(0, s.getLost());
        s.threadSafeIncrementLost();
        assertEquals(1, s.getLost());
    }

    @Test
    public void should_set_error_message(){
        Stats s = new Stats();
        Assert.assertNull(s.getFatalError());
        s.setFatalError("error");
        assertEquals("error", s.getFatalError());
    }

    @Test
    public void should_get_stats_left(){
        Stats s = new Stats();
        assertEquals(Properties.getMaxStatCount(), s.getStatsLeft());

        Context parent = new Context("id", 13);
        parent.addChild(new Context("id2", 133));
        s.store(parent);
        
        assertEquals(Properties.getMaxStatCount() - 3, s.getStatsLeft());
    }

    @Test
    public void should_deep_copy(){
        Stats s1 = StatTestUtil.buildRandomStats(3);
        Stats s2 = s1.deepCopy();

        assertEquals(s1, s2);
    }

    @Test
    public void should_merge_dates(){
        Stats result = new Stats();
        assertEquals(null, result.getStartDate());
        assertEquals(null, result.getEndDate());

        Stats stats1 = new Stats();
        stats1.setStartDate(dt("2000-01-01 08:00:00"));
        stats1.setEndDate(dt("2000-01-01 09:00:00"));
        result.merge(stats1, true);

        //non-null values should override null values
        assertEquals(dt("2000-01-01 08:00:00"), result.getStartDate());
        assertEquals(dt("2000-01-01 09:00:00"), result.getEndDate());

        //smaller range of date will not update result
        Stats stats2 = new Stats();
        stats2.setStartDate(dt("2000-01-01 08:30:00"));
        stats2.setEndDate(dt("2000-01-01 08:40:00"));
        result.merge(stats2, true);

        //no change
        assertEquals(dt("2000-01-01 08:00:00"), result.getStartDate());
        assertEquals(dt("2000-01-01 09:00:00"), result.getEndDate());

        //wider range should update result
        Stats stats3 = new Stats();
        stats3.setStartDate(dt("2000-01-01 07:00:00"));
        stats3.setEndDate(dt("2000-01-01 10:00:00"));
        result.merge(stats3, true);

        //updated
        assertEquals(dt("2000-01-01 07:00:00"), result.getStartDate());
        assertEquals(dt("2000-01-01 10:00:00"), result.getEndDate());
    }

    private Date dt(String date){
        return parseDateTime(date);
    }

    @Test
    public void should_store_child_error_count() {
        Context parent = new Context();
        parent.setId("parent");
        parent.setTime(100);
        parent.setErr(true);

        Context child = new Context();
        child.setId("child");
        child.setTime(10);
        child.setErr(true);

        parent.addChild(child);

        Stats stats = new Stats();

        //when
        stats.store(parent);

        //then
        assertEquals(1, stats.getMap().get("parent").getErr());
        assertEquals(1, stats.getMap().get("parent").getChild("child").getErr());
    }
}