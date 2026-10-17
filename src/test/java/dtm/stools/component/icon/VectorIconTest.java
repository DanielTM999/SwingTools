package dtm.stools.component.icon;

import dtm.stools.component.menu.bar.CollapsibleMenuBar;
import dtm.stools.component.panels.datefield.DatePickerInputField;
import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.search.SearchPanel;
import dtm.stools.component.panels.editor.pdf.ui.PdfIcon;
import dtm.stools.component.panels.editor.sheet.ui.SheetIcon;
import dtm.stools.component.panels.editor.word.ui.WordIcon;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VectorIconTest {
    @Test
    void datePickerUsesAccessibleIconsAndKeepsMonthNavigation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                DatePickerInputField picker = new DatePickerInputField();
                JButton calendar = buttons(picker).stream().filter(b -> b.getIcon() != null).findFirst().orElseThrow();
                assertInstanceOf(VectorIcon.class, calendar.getIcon());
                assertTrue(calendar.getText() == null || calendar.getText().isEmpty());
                assertNotNull(calendar.getAccessibleContext().getAccessibleName());
                var create = DatePickerInputField.class.getDeclaredMethod("createCalendarPopup");
                create.setAccessible(true);
                create.invoke(picker);
                JPopupMenu popup = (JPopupMenu) field(picker, "popupMenu");
                List<JButton> navigation = buttons(popup).stream().filter(b -> b.getIcon() instanceof VectorIcon).toList();
                assertEquals(4, navigation.size());
                for (JButton button : navigation) {
                    assertTrue(button.getText() == null || button.getText().isEmpty());
                    assertFalse(button.getAccessibleContext().getAccessibleName().isBlank());
                }
                YearMonth initial = (YearMonth) field(picker, "currentYearMonth");
                ((JButton) field(picker, "nextButton")).doClick(0);
                assertEquals(initial.plusMonths(1), field(picker, "currentYearMonth"));
                ((JButton) field(picker, "prevButton")).doClick(0);
                assertEquals(initial, field(picker, "currentYearMonth"));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    void missingSearchResourcesKeepIconsAndToggleDirection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea area = new CodeEditorTextArea();
            try {
                SearchPanel search = new SearchPanel(area);
                search.setPrevIconResource("/missing/up.png");
                search.setNextIconResource("/missing/down.png");
                search.setCloseIconResource("/missing/close.png");
                search.setExpandCollapsedIconResource("/missing/right.png");
                search.setExpandExpandedIconResource("/missing/expanded.png");
                for (AbstractButton button : List.of(search.getPrevButton(), search.getNextButton(), search.getCloseButton(), search.getExpandToggle())) {
                    assertInstanceOf(VectorIcon.class, button.getIcon());
                    assertNull(button.getText());
                    assertTrue(Arrays.stream(pixels(button.getIcon(), button, 1)).anyMatch(p -> p != 0));
                }
                int[] collapsed = pixels(search.getExpandToggle().getIcon(), search.getExpandToggle(), 1);
                search.setReplaceVisible(true);
                assertFalse(Arrays.equals(collapsed, pixels(search.getExpandToggle().getIcon(), search.getExpandToggle(), 1)));
                search.setPrevIconResource("/drawables/arrowUpNoTail.png");
                assertNotNull(search.getPrevButton().getIcon());
                assertFalse(search.getPrevButton().getIcon() instanceof VectorIcon);
            } finally {
                area.removeNotify();
            }
        });
    }

    @Test
    void menuMissingResourceUsesVectorFallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CollapsibleMenuBar menu = new CollapsibleMenuBar();
            menu.setCollapseButtonResource("/missing/menu.png");
            assertInstanceOf(VectorIcon.class, menu.getCollapseButton().getIcon());
            assertNull(menu.getCollapseButton().getText());
        });
    }

    @Test
    void iconsPaintAtMultipleScalesAndFollowForeground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLabel component = new JLabel();
            component.setForeground(Color.RED);
            for (VectorIcon.Kind kind : VectorIcon.Kind.values()) {
                Icon icon = new VectorIcon(kind, 20);
                for (int scale : new int[]{1, 2, 3}) {
                    int[] pixels = pixels(icon, component, scale);
                    assertTrue(Arrays.stream(pixels).anyMatch(p -> (p >>> 24) != 0), kind.name());
                    assertTrue(Arrays.stream(pixels).filter(p -> (p >>> 24) != 0).allMatch(p -> (p & 0xFFFFFF) == 0xFF0000), kind.name());
                }
            }
            Icon icon = new VectorIcon(VectorIcon.Kind.CALENDAR, 20);
            int[] red = pixels(icon, component, 1);
            component.setForeground(Color.BLUE);
            assertFalse(Arrays.equals(red, pixels(icon, component, 1)));
            component.setEnabled(false);
            assertFalse(Arrays.equals(pixels(icon, component, 1), red));
        });
    }

    @Test
    void editorSymbolsRenderAndProducePreview() throws Exception {
        List<Icon> icons = new ArrayList<>();
        for (VectorIcon.Kind kind : VectorIcon.Kind.values()) icons.add(new VectorIcon(kind, 32));
        for (String name : List.of("bold", "italic", "underline", "strike", "currency", "function", "autosum", "names", "sort", "percent", "number-format", "what-if", "error-check", "palette")) icons.add(new SheetIcon(name, 32));
        for (String name : List.of("bold", "italic", "ocr", "fill-form", "color-text", "replace")) icons.add(new PdfIcon(name, 32));
        for (String name : List.of("bold", "italic", "underline", "numbering", "equation", "insert.form.text", "insert.field.page", "export.pdf", "template")) icons.add(new WordIcon(name, 32));
        BufferedImage preview = new BufferedImage(480, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
            JLabel component = new JLabel();
            component.setForeground(new Color(0x374151));
            for (int i = 0; i < icons.size(); i++) {
                Icon icon = icons.get(i);
                int[] normal = pixels(icon, component, 2);
                component.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48));
                assertArrayEquals(normal, pixels(icon, component, 2));
                assertTrue(Arrays.stream(normal).anyMatch(p -> (p >>> 24) != 0));
                icon.paintIcon(component, graphics, 8 + i % 10 * 48, 8 + i / 10 * 48);
            }
        } finally {
            graphics.dispose();
        }
        File target = new File("target/vector-icons-preview.png");
        assertTrue(target.getParentFile().isDirectory() || target.getParentFile().mkdirs());
        ImageIO.write(preview, "png", target);
    }

    private static int[] pixels(Icon icon, Component component, int scale) {
        int width = icon.getIconWidth() * scale + 4, height = icon.getIconHeight() * scale + 4;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(scale, scale);
            icon.paintIcon(component, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static List<JButton> buttons(Container parent) {
        List<JButton> buttons = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) buttons.add(button);
            if (child instanceof Container container) buttons.addAll(buttons(container));
        }
        return buttons;
    }
}
