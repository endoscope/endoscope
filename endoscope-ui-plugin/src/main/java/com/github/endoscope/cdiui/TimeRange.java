/*
 * Copyright (c) 2017 SmartRecruiters Inc. All Rights Reserved.
 */

package com.github.endoscope.cdiui;

import java.util.Date;

class TimeRange {
    private Date fromDate = null;
    private Date toDate = null;
    private String instance;
    private String type;
    private boolean includeCurrent = true;

    public TimeRange(Long from, Long to, Long past, String instance, String type) {
        this.instance = instance;
        this.type = type;
        if (past != null) {
            if (past > 0) {
                toDate = new Date();
                fromDate = new Date(toDate.getTime() - past);
            }
        } else {
            if (from != null) {
                fromDate = new Date(from);

                //to requires from
                if (to != null) {
                    toDate = new Date(to);
                    includeCurrent = false;
                } else {
                    toDate = new Date();
                }
            }
        }
    }

    public TimeRange(String from, String to, String past, String instance, String type) {
        this(toLong(from), toLong(to), toLong(past), instance, type);
    }

    private static Long toLong(String value){
        if( value == null || value.trim().length() == 0 ){
            return null;
        }
        return Long.valueOf(value);
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

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isIncludeCurrent() {
        return includeCurrent;
    }

    public void setIncludeCurrent(boolean includeCurrent) {
        this.includeCurrent = includeCurrent;
    }

    @Override
    public String toString() {
        return "TimeRange{" +
                "fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", instance='" + instance + '\'' +
                ", type='" + type + '\'' +
                ", includeCurrent=" + includeCurrent +
                '}';
    }
}
