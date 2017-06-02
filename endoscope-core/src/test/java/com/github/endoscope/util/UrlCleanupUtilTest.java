package com.github.endoscope.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Date: 02/06/2017
 * Time: 16:23
 *
 * @Author p.halicz
 */
@RunWith(Parameterized.class)
public class UrlCleanupUtilTest {
    UrlCleanupUtil util = new UrlCleanupUtil();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "http://domain.com/123/abc", "http://domain.com/[digit]/abc" },
                { "http://domain.com/123/456/x/7/a", "http://domain.com/[digit]/[digit]/x/[digit]/a" },
                { "http://domain.com/123e4567-e89b-12d3-a456-426655440000/abc", "http://domain.com/[uuid]/abc" },
                { "http://domain.com/507f1f77bcf86cd799439011/abc", "http://domain.com/[mongo_id]/abc" },
                { "http://domain.com/jonh@doe.com/abc", "http://domain.com/[email]/abc" },
                { "http://domain.com/abc?q=x&b=t", "http://domain.com/abc" },
                { "http://domain.com/123/a/123e4567-e89b-12d3-a456-426655440000/507f1f77bcf86cd799439011/jonh@doe.com?q=x&b=t", "http://domain.com/[digit]/a/[uuid]/[mongo_id]/[email]" },
                { " x ", "x" },
                { null, null }
        });
    }

    private String input;
    private String expected;

    public UrlCleanupUtilTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void shouldNormalize(){
        String result = util.cleanup(input);
        assertEquals(expected, result);
    }
}