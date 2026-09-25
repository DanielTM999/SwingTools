package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;
import java.util.Set;

public record FilterCriteria(Set<String> values, boolean includeBlanks, List<FilterCondition> conditions, boolean and, Integer top, boolean topPercent,
                             boolean bottom, Integer color, boolean fontColor, String dynamic) {
    public FilterCriteria {
        values = values == null ? null : Set.copyOf(values);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public static FilterCriteria values(Set<String> values, boolean blanks) { return new FilterCriteria(values, blanks, List.of(), true, null, false, false, null, false, null); }
    public static FilterCriteria conditions(List<FilterCondition> conditions, boolean and) { return new FilterCriteria(null, false, conditions, and, null, false, false, null, false, null); }
    public static FilterCriteria top(int n, boolean percent, boolean bottom) { return new FilterCriteria(null, false, List.of(), true, n, percent, bottom, null, false, null); }
    public static FilterCriteria color(int argb, boolean font) { return new FilterCriteria(null, false, List.of(), true, null, false, false, argb, font, null); }
    public static FilterCriteria dynamic(String type) { return new FilterCriteria(null, false, List.of(), true, null, false, false, null, false, type); }
}
