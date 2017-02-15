package com.github.endoscope.util;

import com.github.endoscope.core.Stats;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Date: 15/02/2017
 * Time: 21:02
 *
 * @author p.halicz
 */
public class HistogramUtilTest {
    private Stats stats(long point){
        Stats s = new Stats();
        s.setStartDate(new Date(point));
        s.setEndDate(new Date(point));
        return s;
    }

    @Test(expected = RuntimeException.class)
    public void failForTooLittlePoints(){
        List<Stats> input = Arrays.asList(
                stats(10),
                stats(20),
                stats(30),
                stats(40)
        );

        HistogramUtil.reduce(1, input);
    }

    @Test
    public void returnNullForNullInput(){
        List<Stats> result = HistogramUtil.reduce(2, null);
        Assert.assertNull( result );
    }

    @Test
    public void returnEmptyForEmpty(){
        List<Stats> result = HistogramUtil.reduce(2, emptyList());
        Assert.assertEquals( 0, result.size() );
    }

    @Test
    public void perfectMatch(){
        List<Stats> input = Arrays.asList(
                stats(10),
                stats(20),
                stats(30),
                stats(40)
        );

        List<Stats> result = HistogramUtil.reduce(4, input);

        Assert.assertEquals( 4, result.size() );
        Assert.assertEquals( 10, result.get(0).getStartDate().getTime() );
        Assert.assertEquals( 20, result.get(1).getStartDate().getTime() );
        Assert.assertEquals( 30, result.get(2).getStartDate().getTime() );
        Assert.assertEquals( 40, result.get(3).getStartDate().getTime() );
    }

    @Test
    public void removeRedundant(){
        List<Stats> input = Arrays.asList(
                stats(10),
                stats(11),
                stats(19),
                stats(20),
                stats(22),
                stats(29),
                stats(30),
                stats(40)
        );

        List<Stats> result = HistogramUtil.reduce(4, input);

        Assert.assertEquals( 4, result.size() );
        Assert.assertEquals( 10, result.get(0).getStartDate().getTime() );
        Assert.assertEquals( 20, result.get(1).getStartDate().getTime() );
        Assert.assertEquals( 30, result.get(2).getStartDate().getTime() );
        Assert.assertEquals( 40, result.get(3).getStartDate().getTime() );
    }

    @Test
    public void removeDuplicates(){
        List<Stats> input = Arrays.asList(
                stats(10),
                stats(20),
                stats(40)
        );

        List<Stats> result = HistogramUtil.reduce(10, input);

        Assert.assertEquals( 3, result.size() );
        Assert.assertEquals( 10, result.get(0).getStartDate().getTime() );
        Assert.assertEquals( 20, result.get(1).getStartDate().getTime() );
        Assert.assertEquals( 40, result.get(2).getStartDate().getTime() );
    }
}