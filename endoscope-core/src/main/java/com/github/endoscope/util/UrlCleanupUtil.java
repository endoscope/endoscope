package com.github.endoscope.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class UrlCleanupUtil {
    private List<Replacement> replacements = new CopyOnWriteArrayList();

    private static final Pattern MONGO_ID_PATTERN = Pattern.compile("[0-9a-z]{24}");
    private static final Pattern UUID_PATTERN = Pattern.compile("[a-z0-9\\-]{24,}");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern QUERY_PART_PATTERN = Pattern.compile("\\?.*");
    private static final Pattern EMAIL_PART_PATTERN = Pattern.compile("/[^/]+@[^/]+(?=$|/)");

    public static class Replacement {
        Pattern pattern;
        String replacement;

        public Replacement(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getReplacement() {
            return replacement;
        }
    }

    public UrlCleanupUtil() {
        //default replacements
        addReplacement(MONGO_ID_PATTERN, "[mongo_id]");
        addReplacement(UUID_PATTERN, "[uuid]");
        addReplacement(DIGIT_PATTERN, "[digit]");
        addReplacement(EMAIL_PART_PATTERN, "/[email]");
        addReplacement(QUERY_PART_PATTERN, "");
    }

    public String cleanup(String url) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        try {
            for (Replacement r : replacements) {
                url = r.getPattern().matcher(url).replaceAll(r.getReplacement());
            }
        } catch (Throwable t) {
            //this should not happen ... but just in case don't fail - no matter what
        }
        return url;
    }

    public void addReplacement(Pattern pattern, String replacement) {
        replacements.add(new Replacement(pattern, replacement));
    }
}