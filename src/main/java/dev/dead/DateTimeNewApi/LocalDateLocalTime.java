package dev.dead.DateTimeNewApi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class LocalDateLocalTime {
    static void main(String[] args) {
        LocalDate localdate = LocalDate.now();
        LocalTime localtime = LocalTime.now();
        LocalDateTime localdatetime = localdate.atTime(localtime);

        System.out.println(localdate);
        System.out.println(localtime);
    }
}
