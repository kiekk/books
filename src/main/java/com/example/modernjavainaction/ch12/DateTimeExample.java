package com.example.modernjavainaction.ch12;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public class DateTimeExample {

    public static void main(String[] args) {
        useOldDate();
        useLocalDate();
        useLocalTime();
        useLocalDateTime();
        useInstant();
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

    private static void useLocalTime() {
        LocalTime time = LocalTime.of(13, 45, 20);
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();
        // 13:45:20
        System.out.println(time);
        // hour = 13
        System.out.println("hour = " + hour);
        // minute = 45
        System.out.println("minute = " + minute);
        // second = 20
        System.out.println("second = " + second);
    }

    private static void useLocalDateTime() {
        LocalDate date = LocalDate.of(2014, 3, 18);
        LocalTime time = LocalTime.of(13, 45, 20);

        LocalDateTime dt1 = LocalDateTime.of(2014, Month.MARCH, 18, 13, 45, 20); // 2014-03-18T13:45
        LocalDateTime dt2 = LocalDateTime.of(date, time);
        LocalDateTime dt3 = date.atTime(13, 45, 20);
        LocalDateTime dt4 = date.atTime(time);
        LocalDateTime dt5 = time.atDate(date);
        System.out.println("dt1 : " + dt1);
        System.out.println("dt2 : " + dt2);
        System.out.println("dt3 : " + dt3);
        System.out.println("dt4 : " + dt4);
        System.out.println("dt5 : " + dt5);

        LocalDate date1 = dt1.toLocalDate();
        System.out.println(date1);
        LocalTime time1 = dt1.toLocalTime();
        System.out.println(time1);
    }

    private static void useInstant() {
        LocalTime time = LocalTime.of(13, 45, 20);
        Instant instant = Instant.ofEpochSecond(44 * 365 * 86400);
        Instant now = Instant.now();

        Duration d1 = Duration.between(LocalTime.of(13, 45, 10), time);
        Duration d2 = Duration.between(instant, now);
        System.out.println(d1.getSeconds());
        System.out.println(d2.getSeconds());

        Duration threeMinutes = Duration.of(3, ChronoUnit.MINUTES);
        System.out.println(threeMinutes);

        Period tenDays = Period.between(LocalDate.of(2017, 9, 11),
                LocalDate.of(2017, 9, 21));
        System.out.println(tenDays);

        // Duration, Period 팩토리 메소드로 객체 생성
//        Duration threeMinutes = Duration.ofMinutes(3);
//        Period.ofDays(10);
    }
}
