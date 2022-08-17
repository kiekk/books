package com.example.modernjavainaction.ch12;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;

public class DateTimeExample {

    public static void main(String[] args) {
        useOldDate();
        useLocalDate();
    }

    private static final ThreadLocal<DateFormat> formatters = ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MMM-yyyy"));

    private static void useOldDate() {
        Date date = new Date(114, 2, 18);
        // Tue Mar 18 00:00:00 KST 2014
        System.out.println(date);

        //18-3월-2014
        System.out.println(formatters.get().format(date));

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, Calendar.FEBRUARY, 18);
        // java.util.GregorianCalendar[time=?,areFieldsSet=false,areAllFieldsSet=true,lenient=true,zone=sun.util.calendar.ZoneInfo[id="Asia/Seoul",
        // offset=32400000,dstSavings=0,useDaylight=false,transitions=30,lastRule=null],
        // firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2014,MONTH=1,WEEK_OF_YEAR=34,WEEK_OF_MONTH=3,DAY_OF_MONTH=18,DAY_OF_YEAR=229,DAY_OF_WEEK=4,DAY_OF_WEEK_IN_MONTH=3,AM_PM=1,
        // HOUR=8,HOUR_OF_DAY=20,MINUTE=33,SECOND=26,MILLISECOND=941,ZONE_OFFSET=32400000,DST_OFFSET=0]
        System.out.println(calendar);
    }

    private static void useLocalDate() {
        LocalDate date = LocalDate.of(2014, 3, 18);
        int year = date.getYear();
        Month month = date.getMonth();
        int day = date.getDayOfMonth();
        DayOfWeek dow = date.getDayOfWeek();
        int len = date.lengthOfMonth();
        boolean leap = date.isLeapYear();
        // 2014-03-18
        System.out.println(date);
        // year = 2014
        System.out.println("year = " + year);
        // month = MARCH
        System.out.println("month = " + month);
        // day = 18
        System.out.println("day = " + day);
        // dow = TUESDAY
        System.out.println("dow = " + dow);
        // len = 31
        System.out.println("len = " + len);
        // leap = false
        System.out.println("leap = " + leap);

        int y = date.get(ChronoField.YEAR);
        int m = date.get(ChronoField.MONTH_OF_YEAR);
        int d = date.get(ChronoField.DAY_OF_MONTH);

        // ChronoField Year = 2014
        System.out.println("ChronoField Year = " + y);
        // ChronoField MONTH_OF_YEAR = 3
        System.out.println("ChronoField MONTH_OF_YEAR = " + m);
        // ChronoField DAY_OF_MONTH = 18
        System.out.println("ChronoField DAY_OF_MONTH = " + d);
    }
}
