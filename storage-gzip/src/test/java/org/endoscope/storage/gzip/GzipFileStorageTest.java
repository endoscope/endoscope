package org.endoscope.storage.gzip;

import org.apache.commons.io.FileUtils;
import org.endoscope.core.Stat;
import org.endoscope.core.Stats;
import org.endoscope.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GzipFileStorageTest {
    static File dir;
    static GzipFileStorage ds;
    static Stats stats;
    static JsonUtil jsonUtil = new JsonUtil();

    @Before
    public void before() throws IOException{
        dir = Files.createTempDirectory("DiskStorageTest").toFile();
        ds = new GzipFileStorage(dir);

        stats = buildCommonStats();
    }

    private static Stats buildCommonStats() {
        Stats stats = new Stats();
        stats.setFatalError("error");
        stats.setStartDate(new Date(1000000000000L));
        stats.setEndDate(new Date(1300000000000L));
        stats.setLost(111);
        Stat s1 = new Stat();
        s1.update(123);
        stats.getMap().put("aaa", s1);
        return stats;
    }

    @After
    public void after()throws IOException{
        FileUtils.deleteDirectory(dir);
    }

    @Test
    public void should_save_and_load_part_file() throws IOException{
        String identifier = ds.save(stats);

        assertEquals( "stats_2001-09-09-01-46-40_2011-03-13-07-06-40.gz", identifier );

        Stats loaded = ds.load(identifier);

        assertEquals(stats, loaded);
        assertEquals(jsonUtil.toJson(stats), jsonUtil.toJson(loaded));
    }

    @Test
    public void should_list_only_parts() throws IOException{
        String part1 = ds.save(shiftDates(buildCommonStats(), 1000000L));
        String part2 = ds.save(shiftDates(buildCommonStats(), 2000000L));

        List<GzipFileInfo> info = ds.listParts();
        List<String> names = info.stream().map(i -> i.getName()).collect(toList());

        assertTrue(names.size() >= 2 );//may be more du to different tests
        assertTrue(names.contains(part1));
        assertTrue(names.contains(part2));
    }

    private Stats shiftDates(Stats s, long offset) {
        s.setStartDate(new Date(s.getStartDate().getTime() + offset));
        s.setEndDate(new Date(s.getEndDate().getTime() + offset));
        return s;
    }

    @Test
    public void should_list_only_parts_in_range_1() throws IOException{
        Stats s = buildCommonStats();
        String part1 = ds.save(shiftDates(s, 1000000L));

        String part2 = ds.save(shiftDates(s, 2000000L));
        Date fromDate = s.getStartDate();
        Date toDate = s.getStartDate();

        String part3 = ds.save(shiftDates(s, 3000000L));

        //there might be small time difference due to second precision
        List<GzipFileInfo> info = ds.findParts(new Date(fromDate.getTime()- 1100L), new Date(toDate.getTime()+1100L));

        assertEquals(1, info.size() );
        assertEquals(part2, info.get(0).getName() );
    }

    @Test
    public void should_list_only_parts_in_range_2() throws IOException{
        Stats s = buildCommonStats();
        String part1 = ds.save(shiftDates(s, 1000000L));
        Date fromDate = s.getStartDate();

        String part2 = ds.save(shiftDates(s, 2000000L));
        Date toDate = s.getStartDate();

        String part3 = ds.save(shiftDates(s, 3000000L));

        //there might be small time difference due to second precision
        List<GzipFileInfo> info = ds.findParts(new Date(fromDate.getTime()- 1100L), new Date(toDate.getTime()+1100L));

        assertEquals(2, info.size() );
        assertEquals(part1, info.get(0).getName() );
        assertEquals(part2, info.get(1).getName() );
    }


    @Test
    public void should_list_only_parts_in_range_3() throws IOException{
        Stats s = buildCommonStats();
        String part1 = ds.save(shiftDates(s, 1000000L));
        Date fromDate = s.getStartDate();

        String part2 = ds.save(shiftDates(s, 2000000L));

        String part3 = ds.save(shiftDates(s, 3000000L));
        Date toDate = s.getStartDate();

        //there might be small time difference due to second precision
        List<GzipFileInfo> info = ds.findParts(new Date(fromDate.getTime()- 1100L), new Date(toDate.getTime()+1100L));

        assertEquals(3, info.size() );
        assertEquals(part1, info.get(0).getName() );
        assertEquals(part2, info.get(1).getName() );
        assertEquals(part3, info.get(2).getName() );
    }
}