package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

public final class AutoCompletePopupDefaults {

    private AutoCompletePopupDefaults() {
        throw new AssertionError("Utility class");
    }

    public static AutoCompletePopupFactory intellij() {
        return IntelliJPopup::new;
    }

    public static AutoCompletePopupFactory visualStudioCode() {
        return VisualStudioCodePopup::new;
    }

    public static AutoCompletePopupFactory eclipse() {
        return EclipsePopup::new;
    }

    public static AutoCompletePopupFactory netBeans() {
        return NetBeansPopup::new;
    }

    private abstract static class PresetPopup extends AutoCompletePopup {

        private PresetPopup(JComponent owner) {
            super(owner);
        }

        protected final void applySurface(Color background, Color foreground, Color border) {
            list.setBackground(background);
            scroll.getViewport().setBackground(background);
            loadingPanel.setBackground(background);
            loadingLabel.setForeground(foreground);
            loadingSpinner.setForeground(blend(foreground, background, 0.28f));
            detailArea.setBackground(background);
            detailArea.setForeground(blend(foreground, background, 0.35f));
            detailScroll.setBackground(background);
            detailScroll.getViewport().setBackground(background);
            detailScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, border));
            popup.setBackground(background);
            popup.setBorder(BorderFactory.createLineBorder(border));
        }
    }

    private static final class IntelliJPopup extends PresetPopup {

        private IntelliJPopup(JComponent owner) {
            super(owner);
            Color background = uiColor("PopupMenu.background", "ToolTip.background", Color.WHITE);
            Color foreground = uiColor("PopupMenu.foreground", "ToolTip.foreground", new Color(0x1F2329));
            applySurface(background, foreground, blend(foreground, background, 0.78f));
            setPopupSize(new Dimension(420, 192));
            setDetailMaxHeight(120);
            list.setFixedCellHeight(32);
            list.setVisibleRowCount(6);
            list.setCellRenderer(new IntelliJRenderer());
        }
    }

    private static final class VisualStudioCodePopup extends PresetPopup {

        private VisualStudioCodePopup(JComponent owner) {
            super(owner);
            Color background = uiColor("List.background", "PopupMenu.background", Color.WHITE);
            Color foreground = uiColor("List.foreground", "PopupMenu.foreground", new Color(0x1E1E1E));
            applySurface(background, foreground, blend(foreground, background, 0.72f));
            setPopupSize(new Dimension(480, 216));
            setDetailMaxHeight(132);
            list.setFixedCellHeight(36);
            list.setVisibleRowCount(6);
            list.setCellRenderer(new VisualStudioCodeRenderer());
        }
    }

    private static final class EclipsePopup extends PresetPopup {

        private EclipsePopup(JComponent owner) {
            super(owner);
            Color background = uiColor("List.background", "PopupMenu.background", new Color(0xFAFAFA));
            Color foreground = uiColor("List.foreground", "PopupMenu.foreground", new Color(0x202020));
            applySurface(background, foreground, blend(foreground, background, 0.70f));
            setPopupSize(new Dimension(440, 208));
            setDetailMaxHeight(128);
            list.setFixedCellHeight(26);
            list.setVisibleRowCount(8);
            list.setCellRenderer(new EclipseRenderer());
        }
    }

    private static final class NetBeansPopup extends PresetPopup {

        private NetBeansPopup(JComponent owner) {
            super(owner);
            Color background = uiColor("List.background", "PopupMenu.background", Color.WHITE);
            Color foreground = uiColor("List.foreground", "PopupMenu.foreground", new Color(0x222222));
            applySurface(background, foreground, blend(foreground, background, 0.74f));
            setPopupSize(new Dimension(460, 224));
            setDetailMaxHeight(136);
            list.setFixedCellHeight(32);
            list.setVisibleRowCount(7);
            list.setCellRenderer(new NetBeansRenderer());
        }
    }

    private abstract static class PresetRenderer extends JPanel implements ListCellRenderer<AutoCompleteItem> {
        private final AutoCompleteIcons.Style iconStyle;
        protected final JLabel iconLabel = new JLabel("", SwingConstants.CENTER);
        protected final JLabel titleLabel = new JLabel();
        protected final JLabel detailLabel = new JLabel();
        protected final JLabel kindLabel = new JLabel();

        protected PresetRenderer(AutoCompleteIcons.Style iconStyle) {
            this.iconStyle = iconStyle;
            setOpaque(true);
            iconLabel.setOpaque(true);
        }

        protected final Colors colors(boolean selected) {
            Color background = uiColor("List.background", "PopupMenu.background", Color.WHITE);
            Color foreground = uiColor("List.foreground", "PopupMenu.foreground", new Color(0x1F2329));
            Color selectedBackground = uiColor("List.selectionBackground", "MenuItem.selectionBackground",
                    new Color(0xDCEBFC));
            Color selectedForeground = uiColor("List.selectionForeground", "MenuItem.selectionForeground", foreground);
            Color rowBackground = selected ? selectedBackground : background;
            Color rowForeground = selected ? selectedForeground : foreground;
            return new Colors(rowBackground, rowForeground, blend(rowForeground, rowBackground, 0.48f));
        }

        protected final void updateContent(JList<?> list, AutoCompleteItem item, Colors colors) {
            String label = item == null ? "" : item.label() != null ? item.label() : item.insertText();
            String detail = item == null || item.detail() == null ? "" : item.detail();
            titleLabel.setText(label == null ? "" : label);
            detailLabel.setText(detail);

            Icon icon = item == null ? AutoCompleteIcons.forKind(iconStyle, null) : item.icon();
            if (icon == null) icon = AutoCompleteIcons.forKind(iconStyle, item.kind());
            iconLabel.setIcon(icon);
            iconLabel.setText("");
            iconLabel.setForeground(colors.foreground());
            iconLabel.setBackground(blend(colors.foreground(), colors.background(), 0.86f));
            iconLabel.setFont(list.getFont().deriveFont(Font.BOLD, Math.max(10f, list.getFont().getSize2D() - 1f)));

            titleLabel.setForeground(colors.foreground());
            detailLabel.setForeground(colors.subtle());
            kindLabel.setText(kindText(item));
            kindLabel.setForeground(colors.subtle());
        }
    }

    private static final class IntelliJRenderer extends PresetRenderer {

        private IntelliJRenderer() {
            super(AutoCompleteIcons.Style.INTELLIJ);
            setLayout(new BorderLayout(7, 0));
            iconLabel.setPreferredSize(new Dimension(20, 20));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

            JPanel textPanel = new JPanel(new BorderLayout(6, 0));
            textPanel.setOpaque(false);
            textPanel.add(titleLabel, BorderLayout.CENTER);
            textPanel.add(detailLabel, BorderLayout.EAST);

            kindLabel.setOpaque(true);
            kindLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(kindLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AutoCompleteItem> list,
                                                      AutoCompleteItem item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Colors colors = colors(isSelected);
            setBackground(colors.background());
            updateContent(list, item, colors);
            titleLabel.setFont(list.getFont().deriveFont(Font.PLAIN));
            detailLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(10f, list.getFont().getSize2D() - 1f)));
            kindLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(9f, list.getFont().getSize2D() - 2f)));
            kindLabel.setBackground(blend(colors.foreground(), colors.background(), isSelected ? 0.82f : 0.92f));
            setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
            return this;
        }
    }

    private static final class VisualStudioCodeRenderer extends PresetRenderer {

        private VisualStudioCodeRenderer() {
            super(AutoCompleteIcons.Style.VISUAL_STUDIO_CODE);
            setLayout(new BorderLayout(8, 0));
            iconLabel.setPreferredSize(new Dimension(22, 22));
            iconLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
            textPanel.add(titleLabel);
            textPanel.add(Box.createHorizontalStrut(8));
            textPanel.add(detailLabel);
            textPanel.add(Box.createHorizontalGlue());

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(kindLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AutoCompleteItem> list,
                                                      AutoCompleteItem item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Colors colors = colors(isSelected);
            setBackground(colors.background());
            updateContent(list, item, colors);
            titleLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            detailLabel.setFont(list.getFont().deriveFont(Font.PLAIN));
            kindLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(9f, list.getFont().getSize2D() - 2f)));
            Color accent = uiColor("Component.focusColor", "List.selectionBackground", new Color(0x007ACC));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, isSelected ? 2 : 0, 0, 0, accent),
                    BorderFactory.createEmptyBorder(4, isSelected ? 5 : 7, 4, 8)));
            return this;
        }
    }

    private static final class EclipseRenderer extends PresetRenderer {

        private EclipseRenderer() {
            super(AutoCompleteIcons.Style.INTELLIJ);
            setLayout(new BorderLayout(6, 0));
            iconLabel.setPreferredSize(new Dimension(18, 18));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
            textPanel.add(titleLabel);
            textPanel.add(Box.createHorizontalStrut(5));
            textPanel.add(detailLabel);
            textPanel.add(Box.createHorizontalGlue());

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(kindLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AutoCompleteItem> list,
                                                      AutoCompleteItem item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Colors colors = colors(isSelected);
            setBackground(colors.background());
            updateContent(list, item, colors);
            titleLabel.setFont(list.getFont().deriveFont(Font.PLAIN));
            detailLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(9f, list.getFont().getSize2D() - 1f)));
            detailLabel.setText(detailLabel.getText().isBlank() ? "" : "- " + detailLabel.getText());
            kindLabel.setFont(list.getFont().deriveFont(Font.ITALIC,
                    Math.max(9f, list.getFont().getSize2D() - 2f)));
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 7));
            return this;
        }
    }

    private static final class NetBeansRenderer extends PresetRenderer {

        private NetBeansRenderer() {
            super(AutoCompleteIcons.Style.INTELLIJ);
            setLayout(new BorderLayout(7, 0));
            iconLabel.setPreferredSize(new Dimension(20, 20));

            JPanel textPanel = new JPanel(new BorderLayout(8, 0));
            textPanel.setOpaque(false);
            textPanel.add(titleLabel, BorderLayout.CENTER);
            textPanel.add(detailLabel, BorderLayout.EAST);

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(kindLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AutoCompleteItem> list,
                                                      AutoCompleteItem item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Colors colors = colors(isSelected);
            setBackground(colors.background());
            updateContent(list, item, colors);
            titleLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            detailLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(10f, list.getFont().getSize2D() - 1f)));
            kindLabel.setFont(list.getFont().deriveFont(Font.PLAIN,
                    Math.max(9f, list.getFont().getSize2D() - 2f)));
            kindLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 8));
            return this;
        }
    }

    private static Color uiColor(String primaryKey, String fallbackKey, Color fallback) {
        Color color = UIManager.getColor(primaryKey);
        if (color != null) return color;
        color = UIManager.getColor(fallbackKey);
        return color != null ? color : fallback;
    }

    private static Color blend(Color from, Color to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int red = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int green = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int blue = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int alpha = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(red, green, blue, alpha);
    }

    private static String kindText(AutoCompleteItem item) {
        if (item == null || item.kind() == null) return "text";
        return item.kind().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private record Colors(Color background, Color foreground, Color subtle) {}
}
