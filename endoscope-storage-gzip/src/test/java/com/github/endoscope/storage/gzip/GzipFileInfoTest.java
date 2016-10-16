package com.github.endoscope.storage.gzip;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

public class GzipFileInfoTest {
    private Date dt(String date){
        try {
            return GzipFileInfo.DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void should_format_name_with_all_parts(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(  dt("2001-01-01-01-01-01"));
        info.setInstance("instance");
        info.setType("type");

        String name = info.build();

        assertEquals("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_type_instance.gz",name);
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_to_format_name_without_start_date(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(null);
        info.setToDate(  dt("2001-01-01-01-01-01"));
        info.setInstance("instance");
        info.setType("type");

        info.build();
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_to_format_name_without_end_date(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(null);
        info.setInstance("instance");
        info.setType("type");

        info.build();
    }

    @Test
    public void should_format_name_without_type(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(  dt("2001-01-01-01-01-01"));
        info.setInstance("instance");
        info.setType(null);

        String name = info.build();

        assertEquals("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_null_instance.gz",name);
    }

    @Test
    public void should_format_name_without_instance(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(  dt("2001-01-01-01-01-01"));
        info.setInstance(null);
        info.setType("type");

        String name = info.build();

        assertEquals("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_type_null.gz",name);
    }

    @Test
    public void should_parse_full_name(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_type_instance.gz");

        assertEquals(dt("2000-01-01-01-01-01"), info.getFromDate());
        assertEquals(dt("2001-01-01-01-01-01"), info.getToDate());
        assertEquals("type", info.getType());
        assertEquals("instance", info.getInstance());
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_to_parse_name_with_incorrect_start_date_format(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2000-01-01-01_2001-01-01-01-01-01_type_instance.gz");
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_to_parse_name_with_incorrect_end_date_format(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2000-01-01-01-01-01_2001-01-01_type_instance.gz");
    }

    @Test(expected = RuntimeException.class)
    public void should_fail_to_parse_name_with_start_date_after_end_date(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2010-01-01-01-01-01_2001-01-01-01-01-01_type_instance.gz");
    }

    @Test
    public void should_parse_name_with_null_type(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_null_instance.gz");

        assertEquals(null, info.getType());
        assertEquals("instance", info.getInstance());
    }

    @Test
    public void should_parse_name_with_null_instance(){
        GzipFileInfo info = new GzipFileInfo();

        info.load("stats_2000-01-01-01-01-01_2001-01-01-01-01-01_type_null.gz");

        assertEquals("type", info.getType());
        assertEquals(null, info.getInstance());
    }

    @Test
    public void should_match_empty_critera(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-01"));
        info.setInstance("instance");
        info.setType("type");

        GzipFileInfo emptyInfo = new GzipFileInfo();

        assertTrue(info.match(null, null, null, null));
        assertTrue(emptyInfo.match(null, null, null, null));
    }

    @Test
    public void should_match_start_date_only(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-01"));
        info.setInstance("instance");
        info.setType("type");

        //long before
        assertTrue( info.match(dt("1950-01-01-01-01-01"), null, null, null));
        //exact match
        assertTrue( info.match(dt("2000-01-01-01-01-01"), null, null, null));
        //match date after
        assertFalse(info.match(dt("2000-01-01-01-01-02"), null, null, null));
        assertFalse(info.match(dt("2100-01-01-01-01-01"), null, null, null));
    }

    @Test
    public void should_match_end_date_only(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-02"));
        info.setInstance("instance");
        info.setType("type");

        //long after
        assertTrue( info.match(null, dt("2100-01-01-01-01-01"), null, null));
        //exact match
        assertTrue( info.match(null, dt("2001-01-01-01-01-02"), null, null));
        //match date before
        assertFalse(info.match(null, dt("2001-01-01-01-01-01"), null, null));
        assertFalse(info.match(null, dt("1950-01-01-01-01-01"), null, null));
    }

    @Test
    public void should_match_date_range(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-02"));
        info.setInstance("instance");
        info.setType("type");

        //file entirely in range
        assertTrue( info.match(dt("1950-01-01-01-01-01"), dt("2100-01-01-01-01-02"), null, null));

        //exact match
        assertTrue( info.match(dt("2000-01-01-01-01-01"), dt("2001-01-01-01-01-02"), null, null));

        //file partially in range
        assertFalse(info.match(dt("1950-01-01-01-01-01"), dt("2001-01-01-01-01-01"), null, null));
        assertFalse(info.match(dt("2000-01-01-01-01-02"), dt("2100-01-01-01-01-01"), null, null));

        //file contains range
        assertFalse(info.match(dt("2000-01-01-01-01-02"), dt("2001-01-01-01-01-01"), null, null));
    }

    @Test
    public void should_match_instance_only(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-02"));
        info.setInstance("instance");
        info.setType("type");

        assertTrue( info.match(null, null, "instance", null));
        assertFalse( info.match(null, null, "bazinga", null));
    }

    @Test
    public void should_match_type_only(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-02"));
        info.setInstance("instance");
        info.setType("type");

        assertTrue( info.match(null, null, null, "type"));
        assertFalse( info.match(null, null, null, "bazinga"));
    }

    @Test
    public void should_match_full_criteria(){
        GzipFileInfo info = new GzipFileInfo();
        info.setFromDate(dt("2000-01-01-01-01-01"));
        info.setToDate(dt("2001-01-01-01-01-02"));
        info.setInstance("instance");
        info.setType("type");

        assertTrue( info.match(dt("1950-01-01-01-01-01"), dt("2100-01-01-01-01-01"), "instance", "type"));
    }

    @Test
    public void should_safe_parse(){
        GzipFileInfo info = GzipFileInfo.safeParse("incorect_name");

        assertNull( info );
    }
}