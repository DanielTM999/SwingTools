package dtm.stools.component.panels.editor.code.gutter;

public interface BreakpointListener {
    void onBreakpointToggled(int line, boolean active);
}
