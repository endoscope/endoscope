package com.github.endoscope.storage.jdbc;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * unluckily it's no so easy to pass list of arguments
 *    http://stackoverflow.com/questions/178479/preparedstatement-in-clause-alternatives
 * alternatively consider fixed number of params and set missing to null to have just one query type
 *
 * //TODO consider replacing IN() with subquery with just two dates
 */
public class InParamsUtil {
    private int size = 100;
    private Object emptyValue = null;
    private String inParams;

    public InParamsUtil(int size, Object emptyValue){
        this.size = size;
        this.emptyValue = emptyValue;

        inParams = StringUtils.repeat("?,", size-1) + "?";
    }

    public Object[] fillMissingValues(Object[] values){
        checkSize(values);
        if( values.length == size ){
            return values;
        }
        Object[] result = Arrays.copyOf(values, size);
        if(emptyValue != null){
            Arrays.fill(result, values.length, result.length, emptyValue);
        }
        return result;
    }

    private void checkSize(Object[] values) {
        if( values.length > size ){
            throw new RuntimeException("too many values: " + values.length + " for a fixed IN clause with " + size + " placeholders");
        }
    }

    public Object[] fillMissingValues(Object[] preInValues, Object[] values){
        checkSize(values);

        Object[] result = new Object[preInValues.length + size];
        System.arraycopy(preInValues, 0, result, 0, preInValues.length);
        System.arraycopy(values, 0, result, preInValues.length, values.length);
        Arrays.fill(result, preInValues.length + values.length, result.length, emptyValue);

        return result;
    }

    public int getSize() {
        return size;
    }

    public Object getEmptyValue() {
        return emptyValue;
    }

    public String getInParams() {
        return inParams;
    }
}
