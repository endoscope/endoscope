package org.endoscope.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import static java.util.stream.IntStream.range;
import static org.endoscope.core.StatTestUtil.buildRandomStat;

public class StatTest {

    @Test
    public void should_set_and_get(){
        Stat s = new Stat();
        s.setHits(13);
        Assert.assertEquals(13, s.getHits());

        s.setMax(14);
        Assert.assertEquals(14, s.getMax());

        s.setMin(15);
        Assert.assertEquals(15, s.getMin());

        s.setAvg(16);
        Assert.assertEquals(16, s.getAvg());

        s.setAh10(17);
        Assert.assertEquals(17, s.getAh10());

        Map m = new HashMap<>();
        s.setChildren(m);
        Assert.assertSame(m, s.getChildren());
    }

    @Test
    public void should_set_not_empty_children_if_null(){
        Stat s = new Stat();
        Assert.assertNull(s.getChildren());

        s.ensureChildrenMap();
        Assert.assertNotNull(s.getChildren());

        s.getChildren().put("x", null);

        s.ensureChildrenMap();
        Assert.assertNotNull(s.getChildren());
        Assert.assertTrue(s.getChildren().containsKey("x"));
    }

    @Test
    public void should_get_existing_child(){
        Stat s = new Stat();
        Stat child = s.getChild("x");
        Assert.assertNull(child);

        child = s.createChild("x");
        Assert.assertNotNull(s.getChildren());

        Stat child2 = s.getChild("x");
        Assert.assertSame(child, child2);
    }

    @Test
    public void should_update_stat(){
        Stat s = new Stat();

        s.update(10);
        s.updateAvgHits(100);

        Assert.assertEquals(10, s.getMax());
        Assert.assertEquals(10, s.getMin());
        Assert.assertEquals(10, s.getAvg(), 0.00001);
        Assert.assertEquals(1, s.getHits());
        Assert.assertEquals(1000, s.getAh10());
        Assert.assertEquals(100, s.avgParent, 0.00001);

        s.update(20);
        s.updateAvgHits(200);

        Assert.assertEquals(20, s.getMax());
        Assert.assertEquals(10, s.getMin());
        Assert.assertEquals(15, s.getAvg(), 0.00001);
        Assert.assertEquals(2, s.getHits());
        Assert.assertEquals(1500, s.getAh10());
        Assert.assertEquals(150, s.avgParent, 0.00001);
    }

    @Test
    public void should_not_loose_precision(){
        Stat s = new Stat();
        Random random = new Random();

        range(0, 100000000).forEach( i -> {
            long r = random.nextInt(1000);
            s.update(r);
            s.updateAvgHits(r);
        });

        System.out.println("max: " + s.getMax());
        System.out.println("min: " + s.getMin());
        System.out.println("avg: " + s.getAvg());
        System.out.println("hits: " + s.getHits());
        System.out.println("getAh10(): " + s.getAh10());
        System.out.println("avgParent: " + s.avgParent);

        //with such amount of samples we should be around the 500 - accept 0.5% difference
        Assert.assertTrue(s.getMax() > 995);
        Assert.assertTrue(s.getMin() < 5);
        Assert.assertTrue(s.getAvg() < 505 && s.getAvg() > 445);
        Assert.assertEquals(100000000, s.getHits());
        Assert.assertTrue(s.getAh10() < 5050 && s.getAh10() > 4450 );
        Assert.assertTrue(s.avgParent < 505 && s.avgParent > 445);
    }

    @Test
    public void should_merge_empty_stats(){
        Stat s1 = new Stat();
        Stat s2 = new Stat();

        Assert.assertEquals(s1, s2);

        s1.merge(s2);
        Assert.assertEquals(s1, s2);
    }

    @Test
    public void should_merge_add_to_empty(){
        range(0,10).forEach( i -> {
            Stat s1 = new Stat();
            Stat s2 = buildRandomStat(3);
            s2.setMin(0);

            s1.merge(s2);

            Assert.assertEquals(s1, s2);
        });
    }

    @Test
    public void should_merge_hits(){
        Stat s1 = new Stat(); s1.setHits(10);
        Stat s2 = new Stat(); s2.setHits(13);

        s1.merge(s2);

        Assert.assertEquals(23, s1.getHits());
        Assert.assertEquals(13, s2.getHits());
    }

    @Test
    public void should_merge_parent_count(){
        Stat s1 = new Stat(); s1.setParentCount(10);
        Stat s2 = new Stat(); s2.setParentCount(13);

        s1.merge(s2);

        Assert.assertEquals(23, s1.getParentCount());
        Assert.assertEquals(13, s2.getParentCount());
    }

    @Test
    public void should_merge_max(){
        Stat s1 = new Stat(); s1.setMax(10);
        Stat s2 = new Stat(); s2.setMax(13);

        s1.merge(s2);

        Assert.assertEquals(13, s1.getMax());

        Stat s3 = new Stat(); s3.setMax(13);
        Stat s4 = new Stat(); s4.setMax(10);

        s3.merge(s4);

        Assert.assertEquals(13, s3.getMax());
    }

    @Test
    public void should_merge_min(){
        Stat s1 = new Stat(); s1.setMin(10);
        Stat s2 = new Stat(); s2.setMin(13);

        s1.merge(s2);

        Assert.assertEquals(10, s1.getMin());

        Stat s3 = new Stat(); s3.setMin(13);
        Stat s4 = new Stat(); s4.setMin(10);

        s3.merge(s4);

        Assert.assertEquals(10, s3.getMin());
    }

    @Test
    public void should_merge_avg(){
        Stat s1 = new Stat(); s1.setHits(2); s1.setAvg(10);
        Stat s2 = new Stat(); s2.setHits(2); s2.setAvg(30);

        s1.merge(s2);

        Assert.assertEquals(20, s1.getAvg());
    }

    @Test
    public void should_merge_avg_parent(){
        Stat s1 = new Stat(); s1.setParentCount(2); s1.setAvgParent(10.0);
        Stat s2 = new Stat(); s2.setParentCount(2); s2.setAvgParent(30.0);

        s1.merge(s2);

        Assert.assertEquals(0, Stat.compareDoubleLowPrecision(20.0, s1.getAvgParent()));
    }

    @Test
    public void should_add_child(){
        Stat s1 = new Stat();
        Stat s2 = new Stat();
        Stat child = buildRandomStat(2);
        s2.ensureChildrenMap();
        s2.getChildren().put("child",child);

        s1.merge(s2);

        Assert.assertEquals(s1.getChildren(), s2.getChildren());
        Assert.assertEquals(1, s1.getChildren().size());
        Assert.assertEquals(child, s1.getChildren().get("child"));
    }

    @Test
    public void should_keep_child(){
        Stat s1 = new Stat();
        s1.ensureChildrenMap();
        Stat child = buildRandomStat(2);
        s1.getChildren().put("child",child);

        Stat s2 = new Stat();

        s1.merge(s2);

        Assert.assertNull(s2.getChildren());
        Assert.assertEquals(1, s1.getChildren().size());
        Assert.assertEquals(child, s1.getChildren().get("child"));
    }

    @Test
    public void should_merge_child(){
        Stat child1 = buildRandomStat(2);
        Stat child2 = buildRandomStat(2);

        Stat s1 = new Stat();
        s1.ensureChildrenMap();
        s1.getChildren().put("child",child1);

        Stat s2 = new Stat();
        s2.ensureChildrenMap();
        s2.getChildren().put("child",child2);

        Stat mergedChild = new Stat();
        mergedChild.setMin(child1.getMin());
        mergedChild.merge(child1);
        mergedChild.merge(child2);

        s1.merge(s2);

        Assert.assertEquals(1, s1.getChildren().size());
        Assert.assertEquals(mergedChild, s1.getChildren().get("child"));
    }

    @Test
    public void should_deep_copy(){
        Stat s1 = buildRandomStat(2);
        Stat s2 = s1.deepCopy();

        Assert.assertEquals(s1, s2);
    }
}