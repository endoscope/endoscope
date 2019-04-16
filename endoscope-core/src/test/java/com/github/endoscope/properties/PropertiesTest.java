package com.github.endoscope.properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PropertiesTest {
    private void withProperty(String name, String value, Runnable runnable) {
        String oldValue = System.getProperty(name);
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
        try {
            runnable.run();
        } finally {
            if (oldValue == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, oldValue);
            }
        }
    }

    @Test
    public void should_get_property() {
        withProperty(Properties.SAVE_FREQ_MINUTES, "6", () -> {
            assertEquals(6, Properties.getSaveFreqMinutes());
        });
    }

    @Test
    public void should_use_default_when_no_property() {
        withProperty(Properties.SAVE_FREQ_MINUTES, null, () -> {
            int expected = Integer.parseInt(Properties.DEFAULT_SAVE_FREQ_MINUTES);
            assertEquals(expected, Properties.getSaveFreqMinutes());
        });
    }

    @Test
    public void should_fallback_to_old_property_name() {
        withProperty("endoscope.save.feq.minutes", "7", () -> {
            assertEquals(7, Properties.getSaveFreqMinutes());
        });
    }
}
