package com.github.storage.test;

import java.util.Date;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.AggregatedStorage;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.util.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public abstract class AggregatedStorageTestCases {
    @Mock
    Storage defaultStorage;

    @Mock
    Storage dailyStorage;

    @Mock
    Storage weeklyStorage;

    @Mock
    Storage monthlyStorage;

    AggregatedStorage storage;

    @Captor
    ArgumentCaptor<Stats> statsCaptor;

    public AggregatedStorageTestCases(AggregatedStorage storage){
        this.storage = storage;
    }

    @Before
    public void setup(){
        storage.setStorage(defaultStorage, dailyStorage, weeklyStorage, monthlyStorage);
    }

    private Date parse(String date){
        return DateUtil.parseDateTime(date);
    }

    @Test
    public void shouldDelegateSaveOfOriginalStats(){
        //given
        Stats stats = new Stats();
        stats.setStartDate(parse("2000-03-03 13:15:12"));
        stats.setEndDate(parse("2000-03-03 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(defaultStorage).save(eq(stats), eq("instance"), eq("type"));
    }

    @Test
    public void shouldNotSaveOriginalStats(){
        //given
        Stats stats = new Stats();
        stats.setStartDate(parse("2000-03-03 13:15:12"));
        stats.setEndDate(parse("2000-03-03 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());
        storage.setAggregateOnly(true);

        //when
        storage.save(stats, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
    }

    @Test
    public void shouldCreateNewAggregateForDailyStats(){
        //given
        Stats stats = new Stats();
        stats.setStartDate(parse("2000-03-03 13:15:12"));
        stats.setEndDate(parse("2000-03-03 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(dailyStorage.find(
                eq(parse("2000-03-03 00:00:01")),
                eq(parse("2000-03-03 23:59:58")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                emptyList()
        );

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(dailyStorage).find(eq(parse("2000-03-03 00:00:01")), eq(parse("2000-03-03 23:59:58")), isNull(String.class), eq("type"));
        verify(dailyStorage).replace(isNull(String.class), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2000-03-03 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2000-03-03 23:59:59"), statsCaptor.getValue().getEndDate());
    }

    @Test
    public void shouldUpdateExistingAggregateForDailyStats(){
        //given
        Stats stats = new Stats();
        stats.setLost(3);
        stats.setStartDate(parse("2000-03-03 13:15:12"));
        stats.setEndDate(parse("2000-03-03 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(dailyStorage.find(
                eq(parse("2000-03-03 00:00:01")),
                eq(parse("2000-03-03 23:59:58")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                asList("daily-stat-id")
        );

        Stats existing = new Stats();
        existing.setLost(5);
        //change dates in order to verify that we persist existing again
        existing.setStartDate(parse("2000-03-03 00:00:05"));
        existing.setEndDate(parse("2000-03-03 23:59:50"));
        existing.getMap().put("y", Stat.emptyStat());
        given(dailyStorage.load(eq("daily-stat-id"))).willReturn(existing);

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(dailyStorage).find(eq(parse("2000-03-03 00:00:01")), eq(parse("2000-03-03 23:59:58")), isNull(String.class), eq("type"));
        verify(dailyStorage).replace(eq("daily-stat-id"), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2000-03-03 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2000-03-03 23:59:59"), statsCaptor.getValue().getEndDate());
        assertEquals(8, statsCaptor.getValue().getLost());
        assertTrue(statsCaptor.getValue().getMap().containsKey("x"));
        assertTrue(statsCaptor.getValue().getMap().containsKey("y"));
    }

    @Test
    public void shouldCreateNewAggregateForWeeklyStats(){
        //given
        Stats stats = new Stats();
        stats.setStartDate(parse("2016-10-12 13:15:12"));
        stats.setEndDate(parse("2016-10-12 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(weeklyStorage.find(
                eq(parse("2016-10-09 23:59:59")),
                eq(parse("2016-10-17 00:00:00")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                emptyList()
        );

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(weeklyStorage).find(eq(parse("2016-10-10 00:00:01")), eq(parse("2016-10-16 23:59:58")), isNull(String.class), eq("type"));
        verify(weeklyStorage).replace(isNull(String.class), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2016-10-10 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2016-10-16 23:59:59"), statsCaptor.getValue().getEndDate());
    }

    @Test
    public void shouldUpdateExistingAggregateForWeeklyStats(){
        //given
        Stats stats = new Stats();
        stats.setLost(3);
        stats.setStartDate(parse("2016-10-21 13:15:12"));
        stats.setEndDate(parse("2016-10-21 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(weeklyStorage.find(
                eq(parse("2016-10-17 00:00:01")),
                eq(parse("2016-10-23 23:59:58")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                asList("daily-stat-id")
        );

        Stats existing = new Stats();
        existing.setLost(5);
        //change dates in order to verify that we persist existing again
        existing.setStartDate(parse("2016-10-17 00:00:05"));
        existing.setEndDate(parse("2016-10-23 23:59:50"));
        existing.getMap().put("y", Stat.emptyStat());
        given(weeklyStorage.load(eq("daily-stat-id"))).willReturn(existing);

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(weeklyStorage).find(eq(parse("2016-10-17 00:00:01")), eq(parse("2016-10-23 23:59:58")), isNull(String.class), eq("type"));
        verify(weeklyStorage).replace(eq("daily-stat-id"), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2016-10-17 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2016-10-23 23:59:59"), statsCaptor.getValue().getEndDate());
        assertEquals(8, statsCaptor.getValue().getLost());
        assertTrue(statsCaptor.getValue().getMap().containsKey("x"));
        assertTrue(statsCaptor.getValue().getMap().containsKey("y"));
    }


    @Test
    public void shouldCreateNewAggregateForMonthlyStats(){
        //given
        Stats stats = new Stats();
        stats.setStartDate(parse("2016-10-12 13:15:12"));
        stats.setEndDate(parse("2016-10-12 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(monthlyStorage.find(
                eq(parse("2016-10-01 00:00:01")),
                eq(parse("2016-10-31 23:59:58")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                emptyList()
        );

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(monthlyStorage).find(eq(parse("2016-10-01 00:00:01")), eq(parse("2016-10-31 23:59:58")), isNull(String.class), eq("type"));
        verify(monthlyStorage).replace(isNull(String.class), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2016-10-01 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2016-10-31 23:59:59"), statsCaptor.getValue().getEndDate());
    }

    @Test
    public void shouldUpdateExistingAggregateForMonthlyStats(){
        //given
        Stats stats = new Stats();
        stats.setLost(3);
        stats.setStartDate(parse("2016-10-21 13:15:12"));
        stats.setEndDate(parse("2016-10-21 13:25:07"));
        stats.getMap().put("x", Stat.emptyStat());

        given(monthlyStorage.find(
                eq(parse("2016-10-01 00:00:01")),
                eq(parse("2016-10-31 23:59:58")),
                isNull(String.class),
                eq("type")
        )).willReturn(
                asList("daily-stat-id")
        );

        Stats existing = new Stats();
        existing.setLost(5);
        //change dates in order to verify that we persist existing again
        existing.setStartDate(parse("2016-10-01 00:00:05"));
        existing.setEndDate(parse("2016-10-31 23:59:50"));
        existing.getMap().put("y", Stat.emptyStat());
        given(monthlyStorage.load(eq("daily-stat-id"))).willReturn(existing);

        //when
        storage.save(stats, "instance", "type");

        //then
        verify(monthlyStorage).find(eq(parse("2016-10-01 00:00:01")), eq(parse("2016-10-31 23:59:58")), isNull(String.class), eq("type"));
        verify(monthlyStorage).replace(eq("daily-stat-id"), statsCaptor.capture(), isNull(String.class), eq("type"));

        assertEquals(parse("2016-10-01 00:00:00"), statsCaptor.getValue().getStartDate());
        assertEquals(parse("2016-10-31 23:59:59"), statsCaptor.getValue().getEndDate());
        assertEquals(8, statsCaptor.getValue().getLost());
        assertTrue(statsCaptor.getValue().getMap().containsKey("x"));
        assertTrue(statsCaptor.getValue().getMap().containsKey("y"));
    }

    @Test
    public void shouldFindStatsInDefaultStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-01 23:58:00");

        //when
        storage.find(from, to, "instance", "type");

        //then
        verify(defaultStorage).find(eq(from), eq(to), eq("instance"), eq("type"));
        verifyNoMoreInteractions(dailyStorage);
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldFindStatsInDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-02 01:00:00");

        //when
        storage.find(from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verify(dailyStorage).find(eq(from), eq(to), isNull(String.class), eq("type"));
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldFindStatsInWeeklyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-08 01:00:00");

        //when
        storage.find(from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verifyNoMoreInteractions(dailyStorage);
        verify(weeklyStorage).find(eq(from), eq(to), isNull(String.class), eq("type"));
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldFindStatsInMonthlyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-11-18 01:00:00");

        //when
        storage.find(from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verifyNoMoreInteractions(dailyStorage);
        verifyNoMoreInteractions(weeklyStorage);
        verify(monthlyStorage).find(eq(from), eq(to), isNull(String.class), eq("type"));
    }

    @Test
    public void shouldLoadDetailsFromDefaultStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-01 23:58:00");

        //when
        storage.loadDetails("detailsId", from, to, "instance", "type");

        //then
        verify(defaultStorage).loadDetails(eq("detailsId"), eq(from), eq(to), eq("instance"), eq("type"));
        verifyNoMoreInteractions(dailyStorage);
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadDailyDetailsFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-02 01:58:00");

        //when
        storage.loadDetails("detailsId", from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verify(dailyStorage).loadDetails(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"));
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadWeeklyDetailsFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-08 01:58:00");

        //when
        storage.loadDetails("detailsId", from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verifyNoMoreInteractions(dailyStorage);
        verify(weeklyStorage).loadDetails(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"));
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadMonthlyDetailsFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-11-08 01:58:00");

        //when
        storage.loadDetails("detailsId", from, to, "instance", "type");

        //then
        verifyNoMoreInteractions(defaultStorage);
        verifyNoMoreInteractions(dailyStorage);
        verifyNoMoreInteractions(weeklyStorage);
        verify(monthlyStorage).loadDetails(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"));
    }

    @Test
    public void shouldLoadHistogramFromDefaultStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-01 23:58:00");

        //when
        storage.loadHistogram("detailsId", from, to, "instance", "type", null);

        //then
        verify(defaultStorage).loadHistogram(eq("detailsId"), eq(from), eq(to), eq("instance"), eq("type"), isNull(String.class));
        verifyNoMoreInteractions(dailyStorage);
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadDailyHistogramFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-02 01:58:00");

        //when
        storage.loadHistogram("detailsId", from, to, "instance", "type", null);

        //then
        verifyNoMoreInteractions(defaultStorage);
        verify(dailyStorage).loadHistogram(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"), isNull(String.class));
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadWeeklyHistogramFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-10-08 01:58:00");

        //when
        storage.loadHistogram("detailsId", from, to, "instance", "type", null);

        //then
        verifyNoMoreInteractions(defaultStorage);
        verify(dailyStorage).loadHistogram(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"), isNull(String.class));
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }

    @Test
    public void shouldLoadMonthlyHistogramFromDailyStorage(){
        //given
        Date from = parse("2016-10-01 00:00:00");
        Date to   = parse("2016-11-08 01:58:00");

        //when
        storage.loadHistogram("detailsId", from, to, "instance", "type", null);

        //then
        verifyNoMoreInteractions(defaultStorage);
        verify(dailyStorage).loadHistogram(eq("detailsId"), eq(from), eq(to), isNull(String.class), eq("type"), isNull(String.class));
        verifyNoMoreInteractions(weeklyStorage);
        verifyNoMoreInteractions(monthlyStorage);
    }
}