package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;

import java.util.List;
import java.util.function.Consumer;

public record FilterMenuRequest(String column, List<String> values, FilterCriteria current, boolean numeric, boolean dates, Consumer<FilterCriteria> apply,
                                Runnable sortAscending, Runnable sortDescending, Runnable clear, Runnable custom, Runnable top10) {
    public FilterMenuRequest { values = List.copyOf(values); }
}
