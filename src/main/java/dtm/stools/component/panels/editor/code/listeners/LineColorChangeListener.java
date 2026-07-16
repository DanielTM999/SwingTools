package dtm.stools.component.panels.editor.code.listeners;

import java.awt.Color;

public interface LineColorChangeListener {

    default void onLineColorAdded(int line, Color background, Color foreground) {}

    default void onLineColorRemoved(int line) {}

    default void onLineColorsCleared() {}
}
