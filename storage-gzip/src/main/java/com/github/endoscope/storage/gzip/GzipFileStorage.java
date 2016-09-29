package com.github.endoscope.storage.gzip;

import org.apache.commons.io.IOUtils;
import com.github.endoscope.core.Stats;
import com.github.endoscope.storage.StatsStorage;
import com.github.endoscope.util.JsonUtil;
import org.slf4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple gzip file store. Dumps whole stats to JSON.
 *
 * Search capabilities are for test/demo purposes rather than for practical use on larger stats.
 */
public class GzipFileStorage extends StatsStorage {
    private static final Logger log = getLogger(GzipFileStorage.class);

    public static final String PREFIX = "stats";
    public static final String SEPARATOR = "_";
    public static final String EXTENSION = "gz";
    public static final Pattern NAME_PATTERN = Pattern.compile("stats_....-..-..-..-..-.._....-..-..-..-..-.._null_null\\.gz");
    public static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm-ss";
    public static final String DATE_TIMEZONE = "GMT";
    private File dir;
    private JsonUtil jsonUtil = new JsonUtil();

    public GzipFileStorage(String dir){
        this(toFile(dir));
    }

    private static File toFile(String dir){
        if(dir == null || dir.trim().length() <1){
            throw new IllegalArgumentException("Storage directory cannot be blank");
        }
        return new File(dir);
    }

    public GzipFileStorage(File dir){
        super(null);

        this.dir = dir;
        if( dir.exists() && dir.isFile() ){
            throw new RuntimeException("location exists and is a file - cannot use it as storage directory: " + dir.getAbsolutePath());
        }
        if( !dir.exists() && !dir.mkdirs() ){
            throw new RuntimeException("cannot create storage directory: " + dir.getAbsolutePath());
        }
        log.info("Using storage directory: {}", dir.getAbsolutePath());
    }

    @Override
    public String save(Stats stats) {
        String fileName = buildPartName(stats.getStartDate(), stats.getEndDate());
        try {
            return writeToGzipFile(stats, fileName).getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<GzipFileInfo> listParts(){
        String[] arr = dir.list((dir, name) -> NAME_PATTERN.matcher(name).matches());
        if( arr == null ){
            return new ArrayList<>();
        }
        return toStatsInfo(arr);
    }

    private List<GzipFileInfo> toStatsInfo(String[] arr) {
        return Arrays.asList(arr).stream()
                .sorted()
                .map( name -> safeParseName(name))
                .filter( info -> info != null )
                .collect(Collectors.toList());
    }

    public List<GzipFileInfo> findParts(Date from, Date to) {
        if( from == null || to == null || to.before(from) ){
            return Collections.emptyList();
        }

        String[] arr = dir.list((dir, name) -> {
            if( NAME_PATTERN.matcher(name).matches() ){
                GzipFileInfo info = safeParseName(name);
                return info != null && info.match(from, to, null, null);
            }
            return false;
        });
        return toStatsInfo(arr);
    }

    public Stats load(String name) {
        try {
            return readFromGzipFile(name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read: " + name, e);
        }
    }

    private String buildPartName(Date dateStart, Date dateEnd) {
        DateFormat sdf = getDateFormat();
        return PREFIX + SEPARATOR + sdf.format(dateStart) + SEPARATOR + sdf.format(dateEnd) + "_null_null." + EXTENSION;
    }

    protected DateFormat getDateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone(DATE_TIMEZONE));
        return sdf;
    }

    private GzipFileInfo safeParseName(String name) {
        try{
            GzipFileInfo gzipFileInfo = new GzipFileInfo();
            gzipFileInfo.load(name);
            return gzipFileInfo;
        }catch(Exception e){
            log.warn("Problem parsing stats file name: {}", name, e);
        }
        return null;
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
