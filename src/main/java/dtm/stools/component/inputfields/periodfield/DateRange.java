package dtm.stools.component.inputfields.periodfield;

import java.time.LocalDate;
import java.util.Objects;

public record DateRange(LocalDate start, LocalDate end) implements PeriodValue {
    public DateRange {
        Objects.requireNonNull(start); Objects.requireNonNull(end);
        if (end.isBefore(start)) throw new IllegalArgumentException("end precedes start");
    }
}
