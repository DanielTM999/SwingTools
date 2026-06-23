package dtm.stools.component.panels.editor.code.listeners;

import dtm.stools.component.panels.editor.code.prototype.Breakpoint;

@FunctionalInterface
public interface BreakpointChangeListener {

    void onBreakpointChanged(Breakpoint breakpoint, boolean added);
}
