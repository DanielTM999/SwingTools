package dtm.stools.component.inputfields.periodfield;

/** Immutable local period; dates include both endpoints and no timezone is implied. */
public sealed interface PeriodValue permits DateRange, DateTimeRange, TimeRange {}
