package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record ConditionalRule(ConditionalRuleType type, ComparisonOperator operator, String formula1, String formula2, String text, int rank,
                              boolean percent, boolean bottom, boolean below, boolean equalAverage, int stdDev, TimePeriod timePeriod,
                              DifferentialStyle style, List<Threshold> thresholds, List<Integer> colors, IconSetType iconSet, boolean reverseIcons,
                              boolean showValue, boolean gradient, int priority, boolean stopIfTrue) {
    public ConditionalRule {
        type = Objects.requireNonNullElse(type, ConditionalRuleType.EXPRESSION);
        operator = Objects.requireNonNullElse(operator, ComparisonOperator.GREATER);
        text = Objects.requireNonNullElse(text, "");
        timePeriod = Objects.requireNonNullElse(timePeriod, TimePeriod.TODAY);
        style = Objects.requireNonNullElse(style, DifferentialStyle.EMPTY);
        thresholds = thresholds == null ? List.of() : List.copyOf(thresholds);
        colors = colors == null ? List.of() : List.copyOf(colors);
        iconSet = Objects.requireNonNullElse(iconSet, IconSetType.TRAFFIC_LIGHTS_3);
    }

    public static ConditionalRule cellValue(ComparisonOperator op, String f1, String f2, DifferentialStyle style) {
        return builder().type(ConditionalRuleType.CELL_VALUE).operator(op).formula1(f1).formula2(f2).style(style).showValue(true).build();
    }
    public static ConditionalRule expression(String formula, DifferentialStyle style) {
        return builder().type(ConditionalRuleType.EXPRESSION).formula1(formula).style(style).showValue(true).build();
    }
    public static ConditionalRule colorScale(List<Threshold> thresholds, List<Integer> colors) {
        return builder().type(ConditionalRuleType.COLOR_SCALE).thresholds(thresholds).colors(colors).showValue(true).build();
    }
    public static ConditionalRule twoColorScale(int low, int high) { return colorScale(List.of(Threshold.min(), Threshold.max()), List.of(low, high)); }
    public static ConditionalRule threeColorScale(int low, int mid, int high) { return colorScale(List.of(Threshold.min(), Threshold.percentile(50), Threshold.max()), List.of(low, mid, high)); }
    public static ConditionalRule dataBar(int color, boolean gradient) {
        return builder().type(ConditionalRuleType.DATA_BAR).thresholds(List.of(Threshold.min(), Threshold.max())).colors(List.of(color)).gradient(gradient).showValue(true).build();
    }
    public static ConditionalRule iconSet(IconSetType set) {
        List<Threshold> t = new ArrayList<>();
        for (int i = 0; i < set.size(); i++) t.add(Threshold.percent(Math.round(100.0 * i / set.size())));
        return builder().type(ConditionalRuleType.ICON_SET).iconSet(set).thresholds(t).showValue(true).build();
    }
    public static ConditionalRule top(int rank, boolean percent, boolean bottom, DifferentialStyle style) {
        return builder().type(ConditionalRuleType.TOP_BOTTOM).rank(rank).percent(percent).bottom(bottom).style(style).showValue(true).build();
    }
    public static ConditionalRule text(ConditionalRuleType type, String text, DifferentialStyle style) {
        return builder().type(type).text(text).style(style).showValue(true).build();
    }
    public static ConditionalRule simple(ConditionalRuleType type, DifferentialStyle style) {
        return builder().type(type).style(style).showValue(true).build();
    }
}
