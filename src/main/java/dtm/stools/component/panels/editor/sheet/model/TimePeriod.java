package dtm.stools.component.panels.editor.sheet.model;

public enum TimePeriod {
    TODAY("today"), YESTERDAY("yesterday"), TOMORROW("tomorrow"), LAST_7_DAYS("last7Days"), THIS_WEEK("thisWeek"), LAST_WEEK("lastWeek"), NEXT_WEEK("nextWeek"),
    THIS_MONTH("thisMonth"), LAST_MONTH("lastMonth"), NEXT_MONTH("nextMonth");

    private final String xml;
    TimePeriod(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static TimePeriod fromXml(String v) { for (TimePeriod t : values()) if (t.xml.equals(v)) return t; return TODAY; }
}
