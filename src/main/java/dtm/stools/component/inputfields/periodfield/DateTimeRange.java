package dtm.stools.component.inputfields.periodfield;

import java.time.LocalDateTime;
import java.util.Objects;

public record DateTimeRange(LocalDateTime start, LocalDateTime end) implements PeriodValue {
    public DateTimeRange {
        Objects.requireNonNull(start); Objects.requireNonNull(end);
        if (end.isBefore(start)) throw new IllegalArgumentException("end precedes start");
    }
}
