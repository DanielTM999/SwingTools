package dtm.stools.component.panels.editor.sheet.format;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class DateSerial {
    private static final LocalDate EPOCH_1900 = LocalDate.of(1899, 12, 30);
    private static final LocalDate EPOCH_1904 = LocalDate.of(1904, 1, 1);
    public static final double MAX = 2958465.99999999;

    private DateSerial() {}

    public static double toSerial(LocalDate date, boolean date1904) {
        if (date1904) return ChronoUnit.DAYS.between(EPOCH_1904, date);
        long days = ChronoUnit.DAYS.between(EPOCH_1900, date);
        if (days < 61) days--;
        return days;
    }

    public static double toSerial(LocalDateTime dt, boolean date1904) {
        return toSerial(dt.toLocalDate(), date1904) + dt.toLocalTime().toNanoOfDay() / 86_400_000_000_000.0;
    }

    public static double toSerial(LocalTime time) { return time.toNanoOfDay() / 86_400_000_000_000.0; }

    public static LocalDate toDate(double serial, boolean date1904) {
        long days = (long) Math.floor(serial);
        if (date1904) return EPOCH_1904.plusDays(days);
        if (days < 61) return days <= 0 ? LocalDate.of(1900, 1, 1).minusDays(1) : EPOCH_1900.plusDays(days + 1);
        return EPOCH_1900.plusDays(days);
    }

    public static LocalDateTime toDateTime(double serial, boolean date1904) {
        LocalDate d = toDate(serial, date1904);
        long ms = Math.round((serial - Math.floor(serial)) * 86_400_000.0);
        if (ms >= 86_400_000) { d = d.plusDays(1); ms -= 86_400_000; }
        return LocalDateTime.of(d, LocalTime.ofNanoOfDay(ms * 1_000_000L));
    }

    public static int[] parts(double serial, boolean date1904) {
        long days = (long) Math.floor(serial);
        int year, month, day;
        if (!date1904 && days == 60) { year = 1900; month = 2; day = 29; }
        else if (!date1904 && days == 0) { year = 1900; month = 1; day = 0; }
        else { LocalDate d = toDate(serial, date1904); year = d.getYear(); month = d.getMonthValue(); day = d.getDayOfMonth(); }
        long ms = Math.round((serial - Math.floor(serial)) * 86_400_000.0);
        if (ms >= 86_400_000) ms = 86_399_999;
        int hour = (int) (ms / 3_600_000), minute = (int) (ms / 60_000 % 60), second = (int) (ms / 1000 % 60), millis = (int) (ms % 1000);
        int weekday = (int) (((days % 7) + 6) % 7);
        if (date1904) weekday = (int) (((days + 5) % 7 + 7) % 7);
        else if (days < 60) weekday = (int) (((days - 1) % 7 + 7) % 7);
        return new int[]{year, month, day, hour, minute, second, millis, weekday};
    }

    public static boolean valid(double serial) { return serial >= 0 && serial <= MAX; }
}
