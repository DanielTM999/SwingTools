package dtm.stools.component.panels.tab;

import javax.swing.*;

@FunctionalInterface
public interface TabHeaderFactory {
    JComponent createHeader(TabbedPanel tabs, TabEntry entry, boolean selected);
}
