package com.github.endoscope.storage.gzip;

import com.github.endoscope.core.Stat;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.Filters;
import com.github.endoscope.storage.StatDetails;
import com.github.endoscope.storage.StatHistory;
import com.github.endoscope.storage.Storage;
import com.github.endoscope.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.stream.Collectors.toList;
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
        Stat merged = new Stat();
        List<StatHistory> histogram = statsIds.stream()
                .map( statsId -> load(statsId) )
                .map( stats -> {
                    Map<String, Stat> map = stats.getMap();
                    if( map == null ){
                        return null;
                    }
                    Stat stat = map.get(detailsId);
                    if( stat == null ){
                        return null;
                    }
                    merged.merge(stat);
                    return new StatHistory(stat, stats.getStartDate(), stats.getEndDate());
                })
                .filter( sh -> sh != null && sh.getStartDate() != null )
                .sorted( (sh1, sh2) -> sh1.getStartDate().compareTo(sh2.getStartDate()) )
                .collect(toList());

        if( histogram.isEmpty() ){
            return new StatDetails(detailsId, Stat.emptyStat());
        } else {
            StatDetails result = new StatDetails(detailsId, merged);
            result.setHistogram(histogram);
            return result;
        }
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
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
