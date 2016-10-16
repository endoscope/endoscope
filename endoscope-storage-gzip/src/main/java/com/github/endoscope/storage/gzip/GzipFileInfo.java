package com.github.endoscope.storage.gzip;

import java.beans.Transient;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class GzipFileInfo {
    private static final String PREFIX = "stats";
    private static final String SEPARATOR = "_";
    private static final String EXTENSION = "gz";
    private static final Pattern NAME_PATTERN = Pattern.compile("^stats_([\\d-]+)_([\\d-]+)_([\\w-]+)_([\\w-]+)\\.gz$");
    private static final Pattern NAME_SEGMENT_CLEANUP_PATTERN = Pattern.compile("[^\\w-]");

    public static SimpleDateFormat DATE_FORMAT;

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private Date fromDate;
    private Date toDate;
    private String instance;
    private String type;

    public GzipFileInfo() {
    }

    public GzipFileInfo(Date fromDate, Date toDate, String instance, String type) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.instance = instance;
        this.type = type;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GzipFileInfo)) return false;
        GzipFileInfo that = (GzipFileInfo) o;
        return Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate) &&
                Objects.equals(type, that.type) &&
                Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromDate, toDate, type, instance);
    }

    private boolean inDateRange(Date from, Date to){
        return (from == null || from.getTime() <= getFromDate().getTime() )
            && (to == null || to.getTime() >= getToDate().getTime() );
    }

    private boolean matchType(String type){
        return type == null || type.equals(this.type);
    }

    private boolean matchInstance(String instance){
        return instance == null || instance.equals(this.instance);
    }

    /**
     *
     * @param from if null then ignored, matches when from is lower or equal stats from date
     * @param to if null then ignored, matches when to is greater or equal stats to date
     * @param instance if null then ignored
     * @param type if null then ignored
     * @return
     */
    @Transient
    public boolean match(Date from, Date to, String instance, String type){
        return inDateRange(from, to) && matchType(type) && matchInstance(instance);
    }

    private String wrapNull(String s){
        return defaultString(s, "null");
    }

    private String unwrapNull(String s){
        return "null".equals(s) ? null : s;
    }

    private String cleanupFileNameSegment(String part){
        //name segments cannot contain separators and should contain "safe" characters only
        return NAME_SEGMENT_CLEANUP_PATTERN.matcher(wrapNull(part)).replaceAll("");
    }

    @Transient
    public String build() {
        return PREFIX +
                SEPARATOR + DATE_FORMAT.format(fromDate) +
                SEPARATOR + DATE_FORMAT.format(toDate) +
                SEPARATOR + cleanupFileNameSegment(type) +
                SEPARATOR + cleanupFileNameSegment(instance) +
                "." + EXTENSION;
    }

    @Transient
    public GzipFileInfo load(String name) {
        Matcher m = NAME_PATTERN.matcher(name);
        if( !m.matches() ){
            throw new RuntimeException("Incorrect file name format");
        }
        try {
            Date tmpFromDate = DATE_FORMAT.parse(m.group(1));
            Date tmpToDate = DATE_FORMAT.parse(m.group(2));
            String tmpType = m.group(3);
            String tmpInstance= m.group(4);

            if( tmpFromDate.after(tmpToDate) ){
                throw new RuntimeException("Invalid name format - stats start date is after end date!");
            }
            this.fromDate = tmpFromDate;
            this.toDate = tmpToDate;
            this.type = unwrapNull(tmpType);
            this.instance = unwrapNull(tmpInstance);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Returns null in case of error but doesn't throw exception;
     * @param name
     * @return
     */
    @Transient
    public static GzipFileInfo safeParse(String name) {
        try{
            return new GzipFileInfo().load(name);
        }catch(RuntimeException re){
            return null;
        }
    }
}
