package dtm.stools.component.inputfields.periodfield;

import java.time.LocalTime;
import java.util.Objects;

public record TimeRange(LocalTime start, LocalTime end, boolean nextDay) implements PeriodValue {
    public TimeRange {
        Objects.requireNonNull(start); Objects.requireNonNull(end);
        if (nextDay != end.isBefore(start)) throw new IllegalArgumentException("nextDay must indicate an overnight period");
    }
}
