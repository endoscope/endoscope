package com.github.endoscope.storage.gzip;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple gzip file store. Simply dumps whole stats to JSON.
 *
 * Search capabilities are for test/demo purposes rather than for practical use on larger stats.
 */
public class GzipStorage implements Storage {
    private static final Logger log = getLogger(GzipStorage.class);
    private File dir;
    private JsonUtil jsonUtil = new JsonUtil();

    public void setup(String dirName){
        this.dir = toFile(dirName);
        if( dir.exists() && dir.isFile() ){
            throw new RuntimeException("location exists and is a file - cannot use it as storage directory: " + dir.getAbsolutePath());
        }
        if( !dir.exists() && !dir.mkdirs() ){
            throw new RuntimeException("cannot create storage directory: " + dir.getAbsolutePath());
        }
        log.info("Using storage directory: {}", dir.getAbsolutePath());
    }

    private static File toFile(String dir){
        if(dir == null || dir.trim().length() <1){
            throw new IllegalArgumentException("Storage directory cannot be blank");
        }
        return new File(dir);
    }

    @Override
    public String save(Stats stats, String instance, String type) {
        String fileName = new GzipFileInfo(stats.getStartDate(), stats.getEndDate(), instance, type).build();
        try {
            return writeToGzipFile(stats, fileName).getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String replace(String statsId, Stats stats, String instance, String type) {
        if( isNotBlank(statsId) ){
            File file = buildFile(statsId);
            if( file.exists() ){
                file.delete();
            }
        }
        return save(stats, instance, type);
    }

    @Override
    public Stats load(String statsId) {
        try {
            return readFromGzipFile(statsId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read stats with ID: " + statsId, e);
        }
    }

    @Override
    public List<String> find(Date from, Date to, String instance, String type){
        String[] arr = dir.list((dir, name) -> {
            GzipFileInfo info = GzipFileInfo.safeParse(name);
            return info != null && info.match(from, to, instance, type);
        });
        return Arrays.asList(arr);
    }

    @Override
    public Filters findFilters(Date from, Date to, String type){
        Set<String> types = new HashSet<>();
        Set<String> instances = new HashSet<>();
        dir.list((dir, name) -> {
            GzipFileInfo info = GzipFileInfo.safeParse(name);
            if( info != null && info.match(from, to, null, type)){
                if( info.getInstance() != null ){
                    instances.add(info.getInstance());
                }
                if( info.getType() != null ){
                    types.add(info.getType());
                }
            }
            return false;
        });

        return new Filters(new ArrayList(instances), new ArrayList(types));
    }

    @Override
    public StatDetails loadDetails(String detailsId, List<String> statsIds){
        StatDetails result = new StatDetails(detailsId, null);
        statsIds.stream()
                .map( statsId -> load(statsId) )
                .filter( stats -> stats != null && stats.getStartDate() != null )
                .sorted( (stats1, stats2) -> stats1.getStartDate().compareTo(stats2.getStartDate()) )
                .forEach( stats -> {
                    Map<String, Stat> map = stats.getMap();
                    if( map == null ){
                        return;
                    }
                    Stat stat = map.get(detailsId);
                    if( stat == null ){
                        return;
                    }
                    result.add(stat, stats.getStartDate(),stats.getEndDate());
                });
        return result;
    }

    @Override
    public StatDetails loadDetails(String detailsId, Date from, Date to, String instance, String type){
        List<String> statsIds = find(from, to, instance, type);
        return loadDetails(detailsId, statsIds);
    }

    @Override
    public Stats loadAggregated(boolean topLevelOnly, Date from, Date to, String instance, String type){
        Stats result = new Stats();
        List<String> statsIds = find(from, to, instance, type);
        if( !statsIds.isEmpty() ){
            statsIds.stream()
                    .map( statsId -> load(statsId) )
                    .forEach( stats -> result.merge(stats, !topLevelOnly ) );
        }
        return result;
    }

    private File buildFile(String fileName){
        return new File( dir.getAbsolutePath() + File.separator + fileName);
    }

    private File writeToGzipFile(Stats stats, String fileName) throws IOException {
        File file = buildFile(fileName);
        OutputStream out = null;
        try {
            out = new GZIPOutputStream(new FileOutputStream(file));
            jsonUtil.toJson(stats, out);
            return file;
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private Stats readFromGzipFile(String fileName) throws IOException {
        File file = buildFile(fileName);

        InputStream in = null;
        try {
            in = new GZIPInputStream(new FileInputStream(file));
            return jsonUtil.fromJson(Stats.class, in);
        } catch (FileNotFoundException fnf){
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
