package dtm.stools.component.panels.editor.sheet;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.examples.SheetEditorExample;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static dtm.stools.component.panels.editor.sheet.SheetEditorTest.edt;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SheetVisualSmokeTest {
    @Test
    void rendersEditorInLightAndDarkThemes() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SheetEditor editor = edt(() -> {
            FlatLightLaf.setup();
            SheetEditor e = new SheetEditor();
            e.load(SheetEditorExample.demoWorkbook());
            e.objects().refreshAllPivots();
            e.setSize(1400, 860);
            layout(e);
            e.select(new SheetSelection(dtm.stools.component.panels.editor.sheet.model.CellAddress.parse("C2"), dtm.stools.component.panels.editor.sheet.model.CellAddress.parse("C2"), java.util.List.of(CellRange.parse("C2:E9"))));
            return e;
        });
        try {
            String scale = edt(() -> String.valueOf(Math.round(UIScale.getUserScaleFactor() * 100)));
            Files.createDirectories(Path.of("target"));
            for (boolean dark : new boolean[]{false, true}) {
                for (int sheet = 0; sheet < 3; sheet++) {
                    int s = sheet;
                    BufferedImage image = edt(() -> {
                        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                        SwingUtilities.updateComponentTreeUI(editor);
                        editor.onThemeChanged();
                        editor.activateSheet(s);
                        for (int width : new int[]{1400}) editor.setSize(width, 860);
                        layout(editor);
                        editor.refreshAll();
                        BufferedImage img = new BufferedImage(1400, 860, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = img.createGraphics();
                        try { editor.paint(g); } finally { g.dispose(); }
                        return img;
                    });
                    ImageIO.write(image, "png", Path.of("target", "sheet-ui-" + (dark ? "dark" : "light") + "-sheet" + (sheet + 1) + "-" + scale + ".png").toFile());
                    int distinct = 0, prev = 0;
                    for (int y = 200; y < 700; y += 7) for (int x = 60; x < 1300; x += 11) { int rgb = image.getRGB(x, y); if (rgb != prev) { distinct++; prev = rgb; } }
                    assertTrue(distinct > 40, "o grid deve conter conteúdo pintado");
                }
            }
            for (int width : new int[]{800, 1024}) {
                BufferedImage image = edt(() -> {
                    FlatLightLaf.setup();
                    SwingUtilities.updateComponentTreeUI(editor);
                    editor.onThemeChanged();
                    editor.activateSheet(0);
                    editor.setSize(width, 760);
                    layout(editor);
                    BufferedImage img = new BufferedImage(width, 760, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = img.createGraphics();
                    try { editor.paint(g); } finally { g.dispose(); }
                    return img;
                });
                ImageIO.write(image, "png", Path.of("target", "sheet-ui-light-" + width + "-" + scale + ".png").toFile());
            }
        } finally {
            edt(() -> { editor.close(); FlatLightLaf.setup(); return null; });
        }
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container c) layout(c);
    }
}
