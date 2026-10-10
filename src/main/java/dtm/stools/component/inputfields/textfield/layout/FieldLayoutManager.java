package dtm.stools.component.inputfields.textfield.layout;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

public interface FieldLayoutManager {
    default void install(FieldLayoutTarget field) {}
    default void uninstall(FieldLayoutTarget field) {}
    default void fieldChanged(FieldLayoutTarget field) {}
    default void fieldShown(FieldLayoutTarget field) { fieldChanged(field); }
    default void fieldRemoved(FieldLayoutTarget field) {}
    default void themeChanged(FieldLayoutTarget field) { fieldChanged(field); }

    Insets getInsets(FieldLayoutTarget field);
    Rectangle getFieldBounds(FieldLayoutTarget field);
    Dimension getPreferredSize(FieldLayoutTarget field, Dimension naturalSize);
    default Dimension getMinimumSize(FieldLayoutTarget field, Dimension naturalSize) {
        return getPreferredSize(field, naturalSize);
    }

    default boolean isPlaceholderVisible(FieldLayoutTarget field) { return true; }
    default void paintBackground(Graphics2D g, FieldLayoutTarget field) {}
    default void paintBorder(Graphics2D g, FieldLayoutTarget field) {}
    default void paintOverlay(Graphics2D g, FieldLayoutTarget field) {}
}
