package com.chat.utils;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class DateUtils {
    /**
     * Get the String format "yyyy-MM-dd HH:mm:ss.SSS" time in UTC.
     * @return "yyyy-MM-dd HH:mm:ss.SSS"
     */
    public static String getCurrentTimeStringUTC() {
        String patternStr = "yyyy-MM-dd HH:mm:ss.SSS";
        Date date = new Date();
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"));
        DateFormat dateFormat = new SimpleDateFormat(patternStr);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(date);
    }

    public static int daysBetween(Date smdate, Date bdate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        smdate = sdf.parse(sdf.format(smdate));
        bdate = sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        long betweenDays = (time2 - time1) / (1000 * 3600 * 24);
        return Integer.parseInt(String.valueOf(betweenDays));
    }
    public static int hoursBetween(Date smdate, Date bdate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        smdate = sdf.parse(sdf.format(smdate));
        bdate = sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        long betweenDays = (time2 - time1) / (1000 * 3600 );
        return Integer.parseInt(String.valueOf(betweenDays));
    }

    public static int minuteBetween(Date smdate, Date bdate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        smdate = sdf.parse(sdf.format(smdate));
        bdate = sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        long betweenDays = (time2 - time1) / (1000 * 60 );
        return Integer.parseInt(String.valueOf(betweenDays));
    }

    public static String getToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date time = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(time);

    }
    public static String getSimpleToday() {
        Date time = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yMMdd");
        return sdf.format(time);

    }
    public static Date getExpiredDate(Date date , Integer expiredDays){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, expiredDays);
        return calendar.getTime();
    }
    public static Integer getHour() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int i = calendar.get(Calendar.HOUR_OF_DAY)+1;
        return i;
    }

    /**
     * 返回距离现在多久
     * @param createTime
     */
    public static void getDateDiff(Date createTime) {
    }

    public static String getTimeBefore(Date date) {
        Date now = new Date();
        long l = now.getTime() - date.getTime();
        long day = l / (24 * 60 * 60 * 1000);
        long hour = (l / (60 * 60 * 1000) - day * 24);
        long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long s = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        String r = "";
        if (day > 0) {
            r += day + "day";
        } else if (hour > 0) {
            r += hour + "hour";
        } else if (min > 0) {
            r += min + "minute";
        } else if (s > 0) {
            r += s + "second";
        }
        r += " ago";
        return r;
    }
}
