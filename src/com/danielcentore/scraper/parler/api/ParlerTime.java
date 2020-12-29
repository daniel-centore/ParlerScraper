package com.danielcentore.scraper.parler.api;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Represents a timestamp from Parler. These comprise an ISO8601 UTC timestamp along with a poorly understood extra
 * integer
 *
 * @author Daniel Centore
 */
public class ParlerTime implements Comparable<ParlerTime> {
    final static DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    final static DateFormat SIMPLE_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    final static DateFormat SIMPLE_DATETIME_MS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    final static DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    // e.g. 20201223150519
    final static DateFormat COMPRESSED_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    static {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        SIMPLE_DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        COMPRESSED_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        SIMPLE_DATETIME_MS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    final int year;
    final int month;
    final int day;
    final int hour;
    final int min;
    final int sec;
    final int ms;

    /**
     * Not 100% sure what the purpose of this is, but some poking around suggests that it's probably incremented with
     * each action performed on the site, allowing it to then be used as a deduplicator when multiple actions occur
     * within the same millisecond. It is unclear if it is even utilized at all.
     */
    final int extended;

    public Calendar toCalendar() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month - 1, day, hour, min, sec);
        calendar.set(Calendar.MILLISECOND, ms);
        return calendar;
    }

    public String toParlerTimestamp() {
        return ISO8601_FORMAT.format(toCalendar().getTime()) + "_" + extended;
    }

    public String toSimpleDateTimeFormat() {
        return SIMPLE_DATETIME_FORMAT.format(toCalendar().getTime());
    }
    
    public String toSimpleDateTimeMsFormat() {
        return SIMPLE_DATETIME_MS_FORMAT.format(toCalendar().getTime());
    }
    
    public String toSimpleDateFormat() {
        return SIMPLE_DATE_FORMAT.format(toCalendar().getTime());
    }
    
    
    public String toParlerCompressedTimestamp() {
        return COMPRESSED_FORMAT.format(toCalendar().getTime());
    }

    public Long toUnixTimeMs() {
        return toCalendar().getTimeInMillis();
    }

    /**
     * 
     * @param parlerTimestamp e.g. "2020-12-27T02:27:16.460Z_467811445"
     * @throws ParseException
     */
    public static ParlerTime fromParlerTimestamp(String parlerTimestamp) {
        String[] split = parlerTimestamp.split("_");

        String extended = "0";
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (split.length == 1) {
            try {
                calendar.setTimeInMillis(Long.parseLong(parlerTimestamp));
            } catch (NumberFormatException e) {
                try {
                    Date parsedTime = ISO8601_FORMAT.parse(parlerTimestamp);
                    calendar.setTime(parsedTime);
                } catch (ParseException e2) {
                    throw new RuntimeException(e2);
                }
            }

        } else if (split.length == 2) {
            String iso8601 = split[0];
            extended = split[1];

            try {
                Date parsedTime = ISO8601_FORMAT.parse(iso8601);
                calendar.setTime(parsedTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Timestamp was neither a unix time nor a Parler Extended timestamp");
        }

        return fromCalendar(calendar, extended);
    }
    
    public static ParlerTime fromCompressedParlerTimestamp(String timestamp) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date parsedTime = COMPRESSED_FORMAT.parse(timestamp);
            calendar.setTime(parsedTime);
            return fromCalendar(calendar);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static ParlerTime fromYyyyMmDd(String date) {
        try {
            String[] split = date.split("-");
            return new ParlerTime(
                    Integer.parseInt(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ParlerTime fromUnixTimestampMs(Long ms) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(ms);
        return fromCalendar(calendar, "0");
    }

    public static ParlerTime fromCalendar(Calendar calendar) {
        return fromCalendar(calendar, "0");
    }
    
    public static ParlerTime now() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fromCalendar(calendar, "0");
    }

    public static ParlerTime fromCalendar(Calendar calendar, String extended) {
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return new ParlerTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND),
                extended == null ? null : Integer.parseInt(extended));
    }

    public ParlerTime(int year, int month, int day, int hour, int min, int sec, int ms, int extended) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.ms = ms;
        this.extended = extended;
    }

    public ParlerTime(int year, int month, int day, int hour, int min, int sec, int ms) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.ms = ms;
        this.extended = 0;
    }

    public ParlerTime(int year, int month, int day, int hour, int min, int sec) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.ms = 0;
        this.extended = 0;
    }

    public ParlerTime(int year, int month, int day, int hour, int min) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.min = min;
        this.sec = 0;
        this.ms = 0;
        this.extended = 0;
    }

    public ParlerTime(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = 0;
        this.min = 0;
        this.sec = 0;
        this.ms = 0;
        this.extended = 0;
    }

    @Override
    public int compareTo(ParlerTime o) {
        int result = this.toCalendar().compareTo(o.toCalendar());
        if (result == 0) {
            return Integer.compare(this.extended, o.extended);
        }
        return result;
    }

}
