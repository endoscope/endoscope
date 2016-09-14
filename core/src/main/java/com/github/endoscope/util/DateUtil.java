package com.github.endoscope.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
    public static final SimpleDateFormat DATE_ONLY_GMT;
    static {
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        DATE_ONLY_GMT = new SimpleDateFormat("yyyy_MM_dd");
        DATE_ONLY_GMT.setTimeZone(gmt);
    }

    public Date now(){
        return new Date();
    }

    public static Date startOfADay(Date date){
        if( date == null ){
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));

        return cal.getTime();
    }
}
