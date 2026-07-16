package dtm.stools.component.panels.editor.code.prototype;

import dtm.stools.component.panels.editor.code.prototype.styles.BreakpointStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(of = "line")
public class Breakpoint {

    private final int line;

    @Builder.Default
    private final boolean active = true;

    @Builder.Default
    private final BreakpointStyle style = BreakpointStyle.builder().build();

    private final BreakpointStyle inactiveStyle;

    public BreakpointStyle getEffectiveStyle() {
        return active ? style : getInactiveStyleOrDefault();
    }

    public BreakpointStyle getInactiveStyleOrDefault() {
        if (inactiveStyle != null) {
            return inactiveStyle;
        }
        return BreakpointStyle.builder().color(style.getColor()).build();
    }
}
