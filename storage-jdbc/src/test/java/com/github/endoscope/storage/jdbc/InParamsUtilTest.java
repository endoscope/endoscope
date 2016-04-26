package com.github.endoscope.storage.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class InParamsUtilTest {
    @Test
    public void should_fill_to_in_size_with_empty_value(){
        InParamsUtil util = new InParamsUtil(5, "E");
        Object[] params = new Object[]{"A", "A"};
        Object[] result = util.fillMissingValues(params);

        Assert.assertEquals("A,A,E,E,E", StringUtils.join(result, ","));
    }

    @Test
    public void should_fill_to_in_size_with_null(){
        InParamsUtil util = new InParamsUtil(5, null);
        Object[] params = new Object[]{"A", "A"};
        Object[] result = util.fillMissingValues(params);

        Assert.assertEquals("A,A,,,", StringUtils.join(result, ","));
    }

    @Test
    public void should_fill_to_in_size_with_empty_value_with_pre_params(){
        InParamsUtil util = new InParamsUtil(5, "E");
        Object[] preInParams = new Object[]{"X", "Y", "Z"};
        Object[] params = new Object[]{"A", "A"};
        Object[] result = util.fillMissingValues(preInParams, params);

        Assert.assertEquals("X,Y,Z,A,A,E,E,E", StringUtils.join(result, ","));
    }

    @Test
    public void should_fill_to_in_size_with_null_with_pre_params(){
        InParamsUtil util = new InParamsUtil(5, null);
        Object[] preInParams = new Object[]{"X", "Y", "Z"};
        Object[] params = new Object[]{"A", "A"};
        Object[] result = util.fillMissingValues(preInParams, params);

        Assert.assertEquals("X,Y,Z,A,A,,,", StringUtils.join(result, ","));
    }

}