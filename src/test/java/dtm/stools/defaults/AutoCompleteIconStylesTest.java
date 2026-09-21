package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoCompleteIconStylesTest {

    @Test
    void intellijAndVisualStudioCodeUseDifferentArtworkForEveryKind() {
        for (AutoCompleteItem.Kind kind : AutoCompleteItem.Kind.values()) {
            Icon intellij = AutoCompleteIcons.forKind(AutoCompleteIcons.Style.INTELLIJ, kind);
            Icon visualStudioCode = AutoCompleteIcons.forKind(AutoCompleteIcons.Style.VISUAL_STUDIO_CODE, kind);

            assertNotNull(intellij, () -> "Missing IntelliJ icon for " + kind);
            assertNotNull(visualStudioCode, () -> "Missing VS Code icon for " + kind);
            assertFalse(Arrays.equals(pixels(intellij), pixels(visualStudioCode)),
                    () -> "The presets share the same artwork for " + kind);
        }
    }

    private static int[] pixels(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
