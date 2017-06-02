package com.github.endoscope.util;

import java.util.regex.Pattern;

public class UrlCleanupUtil {
    private static final Pattern MONGO_ID_PATTERN = Pattern.compile("[0-9a-z]{24}");
    private static final Pattern UUID_PATTERN = Pattern.compile("[a-z0-9\\-]{24,}");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern QUERY_PART_PATTERN = Pattern.compile("\\?.*");
    private static final Pattern EMAIL_PART_PATTERN = Pattern.compile("/[^/]+@[^/]+(?=$|/)");

    public String cleanup(String url) {
        if( url == null ){
            return null;
        }
        url = url.trim();
        try{
            url = MONGO_ID_PATTERN.matcher(url).replaceAll("[mongo_id]");
            url = UUID_PATTERN.matcher(url).replaceAll("[uuid]");
            url = DIGIT_PATTERN.matcher(url).replaceAll("[digit]");
            url = EMAIL_PART_PATTERN.matcher(url).replaceAll("/[email]");
            url = QUERY_PART_PATTERN.matcher(url).replaceAll("");
        } catch(Throwable t){
            //this should not happen ... but just in case don't fail - no matter what
        }
        return url;
    }
}