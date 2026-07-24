package DateTimeApis;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import static java.time.temporal.TemporalAdjusters.next;

public class Testing {
    public static void main(String[] args) {
        var instant = Instant.ofEpochSecond(20);
        var date = instant.atZone(java.time.ZoneId.systemDefault());
        System.out.println(date.format(DateTimeFormatter.BASIC_ISO_DATE));
        Duration duration = Duration.between(Instant.MIN, Instant.now());
        System.out.println(duration);

        var bd = LocalDateTime.of(1999, 12, 24, 3, 12);
        System.out.println(bd.withMonth(1));
        System.out.println(bd.plusDays(12));
        System.out.println(bd.with(next(DayOfWeek.FRIDAY)));
        System.out.println(bd.with(new NextWorkingDay()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println(bd.format(formatter));
    }

    /**
     * Advances a date to the next working day:
     * - Friday advances by 3 days (to Monday)
     * - Saturday advances by 2 days (to Monday)
     * - Any other day advances by 1 day
     */
}

class NextWorkingDay implements TemporalAdjuster {

    @Override
    public Temporal adjustInto(Temporal temporal) {
        DayOfWeek dow = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));
        int dayToAdd = 1;

        if (dow == DayOfWeek.FRIDAY) {
            dayToAdd = 3;
        } else if (dow == DayOfWeek.SATURDAY) {
            dayToAdd = 2;
        }

        return temporal.plus(dayToAdd, ChronoUnit.DAYS);
    }
}
