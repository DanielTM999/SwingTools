package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.model.FilterCondition;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.FilterOperator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record FilterJson(Set<String> values, boolean blanks, List<String[]> conditions, boolean and, Integer top, boolean percent, boolean bottom, Integer color, boolean font, String dynamic) {
    public static Map<String, FilterJson> from(Map<Integer, FilterCriteria> criteria) {
        Map<String, FilterJson> out = new HashMap<>();
        criteria.forEach((col, c) -> out.put(String.valueOf(col), new FilterJson(c.values(), c.includeBlanks(),
                c.conditions().stream().map(x -> new String[]{x.operator().name(), x.value()}).toList(), c.and(), c.top(), c.topPercent(), c.bottom(), c.color(), c.fontColor(), c.dynamic())));
        return out;
    }

    public FilterCriteria toModel() {
        return new FilterCriteria(values, blanks, conditions == null ? List.of() : conditions.stream().map(x -> new FilterCondition(FilterOperator.valueOf(x[0]), x[1])).toList(),
                and, top, percent, bottom, color, font, dynamic);
    }
}
