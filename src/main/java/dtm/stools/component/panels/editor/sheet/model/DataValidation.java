package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record DataValidation(List<CellRange> ranges, ValidationType type, ComparisonOperator operator, String formula1, String formula2,
                             boolean allowBlank, boolean showDropdown, ErrorAlertStyle errorStyle, boolean showInput, String inputTitle, String inputMessage,
                             boolean showError, String errorTitle, String errorMessage) {
    public DataValidation {
        ranges = List.copyOf(ranges);
        type = Objects.requireNonNullElse(type, ValidationType.ANY);
        operator = Objects.requireNonNullElse(operator, ComparisonOperator.BETWEEN);
        errorStyle = Objects.requireNonNullElse(errorStyle, ErrorAlertStyle.STOP);
        inputTitle = Objects.requireNonNullElse(inputTitle, "");
        inputMessage = Objects.requireNonNullElse(inputMessage, "");
        errorTitle = Objects.requireNonNullElse(errorTitle, "");
        errorMessage = Objects.requireNonNullElse(errorMessage, "");
    }

    public static DataValidation list(CellRange range, String source) {
        return new DataValidation(List.of(range), ValidationType.LIST, ComparisonOperator.BETWEEN, source, null, true, true, ErrorAlertStyle.STOP, true, "", "", true, "", "");
    }

    public static DataValidation checkbox(CellRange range) {
        return new DataValidation(List.of(range), ValidationType.CHECKBOX, ComparisonOperator.BETWEEN, null, null, true, false, ErrorAlertStyle.STOP, false, "", "", true, "", "");
    }

    public boolean appliesTo(int row, int column) { for (CellRange r : ranges) if (r.contains(row, column)) return true; return false; }
}
