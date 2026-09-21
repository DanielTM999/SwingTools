package dtm.stools.component.popup;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModernDialogTest {

    @Test
    void keepsNaturalSizeWhenItFitsInsideScreen() {
        Dimension result = ModernDialog.constrainedSize(
                new Dimension(420, 260),
                new Rectangle(0, 0, 1920, 1080)
        );

        assertEquals(new Dimension(420, 260), result);
    }

    @Test
    void limitsBothDimensionsLeavingAMarginAroundDialog() {
        Dimension result = ModernDialog.constrainedSize(
                new Dimension(2000, 1200),
                new Rectangle(0, 0, 1280, 720)
        );

        assertEquals(new Dimension(1248, 688), result);
    }
}
