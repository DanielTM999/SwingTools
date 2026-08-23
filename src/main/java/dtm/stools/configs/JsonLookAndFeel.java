package dtm.stools.configs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class JsonLookAndFeel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> THEME_TYPE = new TypeReference<>() {};

    private static final Map<String, String> GLOBAL_COLOR_KEYS = Map.ofEntries(
            Map.entry("background", "Panel.background"),
            Map.entry("foreground", "Label.foreground"),
            Map.entry("text", "Label.foreground"),
            Map.entry("primary", "Button.background"),
            Map.entry("accent", "Button.select"),
            Map.entry("danger", "OptionPane.errorDialog.titlePane.background"),
            Map.entry("border", "Component.borderColor"),
            Map.entry("selectionBackground", "List.selectionBackground"),
            Map.entry("selectionForeground", "List.selectionForeground"),
            Map.entry("disabledForeground", "Label.disabledForeground")
    );

    private static final Map<String, List<String>> COMPONENT_ALIASES = Map.ofEntries(
            Map.entry("background", List.of("background")),
            Map.entry("foreground", List.of("foreground")),
            Map.entry("text", List.of("foreground")),
            Map.entry("selectionBackground", List.of("selectionBackground")),
            Map.entry("selectionForeground", List.of("selectionForeground")),
            Map.entry("disabledBackground", List.of("disabledBackground")),
            Map.entry("disabledForeground", List.of("disabledForeground")),
            Map.entry("caret", List.of("caretForeground")),
            Map.entry("caretForeground", List.of("caretForeground")),
            Map.entry("font", List.of("font")),
            Map.entry("border", List.of("border", "borderColor")),
            Map.entry("borderColor", List.of("borderColor")),
            Map.entry("padding", List.of("margin")),
            Map.entry("margin", List.of("margin")),
            Map.entry("rowHeight", List.of("rowHeight")),
            Map.entry("arc", List.of("arc")),
            Map.entry("borderRadius", List.of("arc"))
    );

    private JsonLookAndFeel() {
    }

    public static void apply(String json) {
        apply(json, true);
    }

    public static void apply(String json, boolean updateOpenWindows) {
        Objects.requireNonNull(json, "json");
        try {
            applyObject(OBJECT_MAPPER.readValue(json, THEME_TYPE), updateOpenWindows);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid theme JSON.", e);
        }
    }

    public static void apply(Path path) {
        apply(path, true);
    }

    public static void apply(Path path, boolean updateOpenWindows) {
        try {
            applyObject(OBJECT_MAPPER.readValue(path.toFile(), THEME_TYPE), updateOpenWindows);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read theme file: " + path, e);
        }
    }

    public static void apply(File file) {
        apply(file.toPath(), true);
    }

    public static void apply(File file, boolean updateOpenWindows) {
        try {
            applyObject(OBJECT_MAPPER.readValue(file, THEME_TYPE), updateOpenWindows);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read theme file: " + file, e);
        }
    }

    public static void apply(InputStream inputStream) {
        apply(inputStream, true);
    }

    public static void apply(InputStream inputStream, boolean updateOpenWindows) {
        try (inputStream) {
            applyObject(OBJECT_MAPPER.readValue(inputStream, THEME_TYPE), updateOpenWindows);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read theme stream.", e);
        }
    }

    public static void apply(Map<String, Object> theme) {
        apply(theme, true);
    }

    public static void apply(Map<String, Object> theme, boolean updateOpenWindows) {
        applyObject(theme, updateOpenWindows);
    }

    public static void updateOpenWindows() {
        UiTokens.refresh();
        Runnable update = () -> {
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                applyDefaultsToTree(window);
                window.invalidate();
                window.validate();
                window.repaint();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    private static void applyObject(Map<String, Object> theme, boolean updateOpenWindows) {
        Object lookAndFeel = theme.get("lookAndFeel");
        if (lookAndFeel instanceof String className && !className.isBlank()) {
            setLookAndFeel(className);
        }

        Font baseFont = parseFont(theme.get("font"), UIManager.getFont("Label.font"));
        if (baseFont != null) {
            applyBaseFont(baseFont);
        }

        Object colors = theme.get("colors");
        if (colors instanceof Map<?, ?> map) {
            applyColors(castMap(map));
        }

        Object components = theme.get("components");
        if (components instanceof Map<?, ?> map) {
            applyComponents(castMap(map));
        }

        Object ui = theme.get("ui");
        if (ui instanceof Map<?, ?> map) {
            applyDirectUi(castMap(map));
        }

        UiTokens.refresh();

        if (updateOpenWindows) {
            updateOpenWindows();
        }
    }

    public static void applyDefaultsToTree(Component component) {
        if (component == null) return;
        applyDefaults(component);

        if (component instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                applyDefaultsToTree(menu.getItem(i));
            }
        }

        if (component instanceof JMenuBar menuBar) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                applyDefaultsToTree(menuBar.getMenu(i));
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyDefaultsToTree(child);
            }
        }
    }

    private static void applyDefaults(Component component) {
        if (component instanceof JTable table) {
            applyComponentDefaults(table, "Table");
            Integer rowHeight = getInteger("Table.rowHeight");
            if (rowHeight != null && rowHeight > 0) table.setRowHeight(rowHeight);
            table.setGridColor(colorOrCurrent("Table.gridColor", table.getGridColor()));
            JTableHeader header = table.getTableHeader();
            if (header != null) applyComponentDefaults(header, "TableHeader");
            return;
        }

        if (component instanceof JTextField textField) {
            applyComponentDefaults(textField, "TextField");
            textField.setCaretColor(colorOrCurrent("TextField.caretForeground", textField.getCaretColor()));
            return;
        }

        if (component instanceof JTextArea textArea) {
            applyComponentDefaults(textArea, "TextArea");
            textArea.setCaretColor(colorOrCurrent("TextArea.caretForeground", textArea.getCaretColor()));
            return;
        }

        if (component instanceof JTextPane textPane) {
            applyComponentDefaults(textPane, "TextPane");
            textPane.setCaretColor(colorOrCurrent("TextPane.caretForeground", textPane.getCaretColor()));
            return;
        }

        if (component instanceof JEditorPane editorPane) {
            applyComponentDefaults(editorPane, "EditorPane");
            editorPane.setCaretColor(colorOrCurrent("EditorPane.caretForeground", editorPane.getCaretColor()));
            return;
        }

        if (component instanceof JButton) {
            applyComponentDefaults(component, "Button");
            return;
        }

        if (component instanceof JToggleButton) {
            applyComponentDefaults(component, "ToggleButton");
            return;
        }

        if (component instanceof JCheckBox) {
            applyComponentDefaults(component, "CheckBox");
            return;
        }

        if (component instanceof JRadioButton) {
            applyComponentDefaults(component, "RadioButton");
            return;
        }

        if (component instanceof JComboBox<?>) {
            applyComponentDefaults(component, "ComboBox");
            return;
        }

        if (component instanceof JLabel) {
            applyComponentDefaults(component, "Label");
            return;
        }

        if (component instanceof JPanel) {
            applyComponentDefaults(component, "Panel");
            return;
        }

        if (component instanceof JScrollPane) {
            applyComponentDefaults(component, "ScrollPane");
            return;
        }

        if (component instanceof JViewport) {
            applyComponentDefaults(component, "Viewport");
            return;
        }

        if (component instanceof JList<?>) {
            applyComponentDefaults(component, "List");
            return;
        }

        if (component instanceof JTree) {
            applyComponentDefaults(component, "Tree");
            return;
        }

        if (component instanceof JMenuBar) {
            applyComponentDefaults(component, "MenuBar");
            return;
        }

        if (component instanceof JMenu) {
            applyComponentDefaults(component, "Menu");
            return;
        }

        if (component instanceof JMenuItem) {
            applyComponentDefaults(component, "MenuItem");
            return;
        }

        if (component instanceof JToolBar) {
            applyComponentDefaults(component, "ToolBar");
            return;
        }

        if (component instanceof JTabbedPane) {
            applyComponentDefaults(component, "TabbedPane");
            return;
        }

        if (component instanceof JSplitPane) {
            applyComponentDefaults(component, "SplitPane");
        }
    }

    private static void applyComponentDefaults(Component component, String prefix) {
        component.setBackground(colorOrCurrent(prefix + ".background", component.getBackground()));
        component.setForeground(colorOrCurrent(prefix + ".foreground", component.getForeground()));

        Font font = UIManager.getFont(prefix + ".font");
        if (font != null) component.setFont(font);

        if (component instanceof JComponent jComponent) {
            Border border = UIManager.getBorder(prefix + ".border");
            if (border != null) jComponent.setBorder(border);

            Insets margin = UIManager.getInsets(prefix + ".margin");
            if (margin != null && component instanceof AbstractButton button) {
                button.setMargin(margin);
            }
            if (margin != null && component instanceof JTextField textField) {
                textField.setMargin(margin);
            }
        }
    }

    private static void setLookAndFeel(String className) {
        try {
            UIManager.setLookAndFeel(className);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to apply look and feel: " + className, e);
        }
    }

    private static void applyBaseFont(Font font) {
        for (String key : List.of(
                "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
                "ColorChooser.font", "ComboBox.font", "Label.font", "List.font",
                "MenuBar.font", "MenuItem.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font",
                "Menu.font", "PopupMenu.font", "OptionPane.font", "Panel.font",
                "ProgressBar.font", "ScrollPane.font", "Viewport.font", "TabbedPane.font",
                "Table.font", "TableHeader.font", "TextField.font", "PasswordField.font",
                "TextArea.font", "TextPane.font", "EditorPane.font", "TitledBorder.font",
                "ToolBar.font", "ToolTip.font", "Tree.font"
        )) {
            UIManager.put(key, font);
        }
    }

    private static void applyColors(Map<String, Object> colors) {
        for (Map.Entry<String, Object> entry : colors.entrySet()) {
            Color color = parseColor(entry.getValue());
            if (color == null) continue;

            String uiKey = GLOBAL_COLOR_KEYS.get(entry.getKey());
            if (uiKey != null) {
                UIManager.put(uiKey, color);
            }
            UIManager.put("SwingTools.color." + entry.getKey(), color);
        }

        Color background = getColor("SwingTools.color.background");
        Color foreground = getColor("SwingTools.color.foreground", getColor("SwingTools.color.text"));
        Color primary = getColor("SwingTools.color.primary", getColor("SwingTools.color.accent"));
        Color border = getColor("SwingTools.color.border");
        Color selectionBg = getColor("SwingTools.color.selectionBackground", primary);
        Color selectionFg = getColor("SwingTools.color.selectionForeground");

        putIfPresent(background, "Panel.background", "Viewport.background", "ScrollPane.background",
                "TextArea.background", "TextPane.background", "EditorPane.background",
                "Table.background", "Tree.background", "List.background", "Menu.background",
                "MenuItem.background", "MenuBar.background", "PopupMenu.background",
                "OptionPane.background", "ToolBar.background", "TabbedPane.background",
                "SplitPane.background");
        putIfPresent(foreground, "Label.foreground", "Panel.foreground", "Button.foreground",
                "TextField.foreground", "TextArea.foreground", "TextPane.foreground",
                "EditorPane.foreground", "Table.foreground", "Tree.foreground", "List.foreground",
                "Menu.foreground", "MenuItem.foreground", "MenuBar.foreground",
                "OptionPane.foreground", "CheckBox.foreground", "RadioButton.foreground",
                "ComboBox.foreground", "ToolBar.foreground", "TabbedPane.foreground");
        putIfPresent(primary, "Button.background", "Button.select", "ToggleButton.select",
                "CheckBox.select", "RadioButton.select", "ComboBox.buttonBackground");
        putIfPresent(border, "Component.borderColor", "Separator.foreground", "Table.gridColor");
        putIfPresent(selectionBg, "List.selectionBackground", "Table.selectionBackground",
                "Tree.selectionBackground", "TextField.selectionBackground", "TextArea.selectionBackground",
                "ComboBox.selectionBackground");
        putIfPresent(selectionFg, "List.selectionForeground", "Table.selectionForeground",
                "Tree.selectionForeground", "TextField.selectionForeground", "TextArea.selectionForeground",
                "ComboBox.selectionForeground");
    }

    private static void applyComponents(Map<String, Object> components) {
        for (Map.Entry<String, Object> component : components.entrySet()) {
            if (!(component.getValue() instanceof Map<?, ?> rawProps)) continue;

            String componentName = component.getKey();
            Map<String, Object> props = castMap(rawProps);
            for (Map.Entry<String, Object> prop : props.entrySet()) {
                Object value = parseUiValue(prop.getValue(), prop.getKey());
                if (value == null) continue;

                List<String> aliases = COMPONENT_ALIASES.getOrDefault(prop.getKey(), List.of(prop.getKey()));
                for (String alias : aliases) {
                    UIManager.put(componentName + "." + alias, value);
                }
            }
        }
    }

    private static void applyDirectUi(Map<String, Object> ui) {
        for (Map.Entry<String, Object> entry : ui.entrySet()) {
            Object value = parseUiValue(entry.getValue(), entry.getKey());
            if (value != null) {
                UIManager.put(entry.getKey(), value);
            }
        }
    }

    private static Object parseUiValue(Object value, String key) {
        if (value == null) return null;
        String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> object = castMap(map);
            if (object.containsKey("family") || object.containsKey("size") || object.containsKey("style")) {
                return parseFont(object, UIManager.getFont("Label.font"));
            }
            if (object.containsKey("top") || object.containsKey("left") || object.containsKey("bottom") || object.containsKey("right")) {
                return parseInsets(object);
            }
            if (object.containsKey("width") || object.containsKey("height")) {
                return parseDimension(object);
            }
            if (object.containsKey("color")) {
                return parseColor(object.get("color"));
            }
            return object;
        }

        if (value instanceof List<?> list) {
            if (normalizedKey.contains("margin") || normalizedKey.contains("padding") || normalizedKey.contains("inset")) {
                return parseInsets(list);
            }
            if (normalizedKey.contains("size") || normalizedKey.contains("dimension")) {
                return parseDimension(list);
            }
            return value;
        }

        if (value instanceof String text) {
            Color color = parseColor(text);
            if (color != null) return color;
            return text;
        }

        if (value instanceof Number number) {
            if (normalizedKey.contains("border")) {
                return BorderFactory.createLineBorder(Color.GRAY, Math.max(1, number.intValue()));
            }
            return number.intValue() == number.doubleValue() ? number.intValue() : number.doubleValue();
        }

        return value;
    }

    private static Font parseFont(Object value, Font fallback) {
        if (value == null) return fallback;
        if (value instanceof String family) {
            int size = fallback == null ? 13 : fallback.getSize();
            int style = fallback == null ? Font.PLAIN : fallback.getStyle();
            return new Font(family, style, size);
        }
        if (!(value instanceof Map<?, ?> raw)) return fallback;

        Map<String, Object> font = castMap(raw);
        String family = stringValue(font.get("family"), fallback == null ? Font.SANS_SERIF : fallback.getFamily());
        int size = intValue(font.get("size"), fallback == null ? 13 : fallback.getSize());
        int style = parseFontStyle(font.get("style"), fallback == null ? Font.PLAIN : fallback.getStyle());
        return new Font(family, style, size);
    }

    private static int parseFontStyle(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();

        String text = value.toString().toLowerCase(Locale.ROOT);
        int style = Font.PLAIN;
        if (text.contains("bold")) style |= Font.BOLD;
        if (text.contains("italic")) style |= Font.ITALIC;
        return style;
    }

    private static Insets parseInsets(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> map = castMap(raw);
            return new Insets(
                    intValue(map.get("top"), 0),
                    intValue(map.get("left"), 0),
                    intValue(map.get("bottom"), 0),
                    intValue(map.get("right"), 0)
            );
        }
        if (value instanceof List<?> list) {
            if (list.size() == 1) {
                int all = intValue(list.getFirst(), 0);
                return new Insets(all, all, all, all);
            }
            if (list.size() == 2) {
                int vertical = intValue(list.get(0), 0);
                int horizontal = intValue(list.get(1), 0);
                return new Insets(vertical, horizontal, vertical, horizontal);
            }
            if (list.size() >= 4) {
                return new Insets(
                        intValue(list.get(0), 0),
                        intValue(list.get(1), 0),
                        intValue(list.get(2), 0),
                        intValue(list.get(3), 0)
                );
            }
        }
        int all = intValue(value, 0);
        return new Insets(all, all, all, all);
    }

    private static Dimension parseDimension(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> map = castMap(raw);
            return new Dimension(intValue(map.get("width"), 0), intValue(map.get("height"), 0));
        }
        if (value instanceof List<?> list && list.size() >= 2) {
            return new Dimension(intValue(list.get(0), 0), intValue(list.get(1), 0));
        }
        int size = intValue(value, 0);
        return new Dimension(size, size);
    }

    private static Color parseColor(Object value) {
        if (value instanceof Color color) return color;
        if (!(value instanceof String raw)) return null;

        String text = raw.trim();
        if (text.isEmpty()) return null;

        if (text.startsWith("#")) {
            return parseHexColor(text.substring(1));
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgb(") && lower.endsWith(")")) {
            List<String> parts = splitArgs(text.substring(4, text.length() - 1));
            return new Color(
                    parseColorChannel(parts, 0, 0),
                    parseColorChannel(parts, 1, 0),
                    parseColorChannel(parts, 2, 0)
            );
        }
        if (lower.startsWith("rgba(") && lower.endsWith(")")) {
            List<String> parts = splitArgs(text.substring(5, text.length() - 1));
            return new Color(
                    parseColorChannel(parts, 0, 0),
                    parseColorChannel(parts, 1, 0),
                    parseColorChannel(parts, 2, 0),
                    parseAlpha(parts, 3, 255)
            );
        }

        return switch (lower) {
            case "black" -> Color.BLACK;
            case "white" -> Color.WHITE;
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "gray", "grey" -> Color.GRAY;
            case "lightgray", "lightgrey" -> Color.LIGHT_GRAY;
            case "darkgray", "darkgrey" -> Color.DARK_GRAY;
            case "transparent" -> new Color(0, 0, 0, 0);
            default -> null;
        };
    }

    private static Color parseHexColor(String hex) {
        String value = hex.trim();
        if (value.length() == 3) {
            value = "" + value.charAt(0) + value.charAt(0)
                    + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2);
        } else if (value.length() == 4) {
            value = "" + value.charAt(0) + value.charAt(0)
                    + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2)
                    + value.charAt(3) + value.charAt(3);
        }

        try {
            if (value.length() == 6) {
                return new Color(Integer.parseInt(value, 16));
            }
            if (value.length() == 8) {
                long rgba = Long.parseLong(value, 16);
                int r = (int) ((rgba >> 24) & 0xff);
                int g = (int) ((rgba >> 16) & 0xff);
                int b = (int) ((rgba >> 8) & 0xff);
                int a = (int) (rgba & 0xff);
                return new Color(r, g, b, a);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    private static int parseColorChannel(List<String> parts, int index, int fallback) {
        if (index >= parts.size()) return fallback;
        return clamp((int) Math.round(Double.parseDouble(parts.get(index).trim())), 0, 255);
    }

    private static int parseAlpha(List<String> parts, int index, int fallback) {
        if (index >= parts.size()) return fallback;
        double value = Double.parseDouble(parts.get(index).trim());
        if (value <= 1) value *= 255;
        return clamp((int) Math.round(value), 0, 255);
    }

    private static List<String> splitArgs(String value) {
        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            parts.add(part.trim());
        }
        return parts;
    }

    private static Color getColor(String key) {
        return getColor(key, null);
    }

    private static Color getColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private static Color colorOrCurrent(String key, Color current) {
        Color color = UIManager.getColor(key);
        return color == null ? current : color;
    }

    private static Integer getInteger(String key) {
        Object value = UIManager.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static void putIfPresent(Object value, String... keys) {
        if (value == null) return;
        for (String key : keys) {
            UIManager.put(key, value);
        }
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public static Border emptyBorder(Object value) {
        Insets insets = parseInsets(value);
        return new EmptyBorder(insets);
    }
}
