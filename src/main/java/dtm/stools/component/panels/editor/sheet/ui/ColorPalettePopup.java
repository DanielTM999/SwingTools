package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;

public final class ColorPalettePopup {
    private static final int[] STANDARD = {0xFFC00000, 0xFFFF0000, 0xFFFFC000, 0xFFFFFF00, 0xFF92D050, 0xFF00B050, 0xFF00B0F0, 0xFF0070C0, 0xFF002060, 0xFF7030A0};

    private ColorPalettePopup() {}

    public static void show(Component invoker, int x, int y, SheetTheme theme, String noneLabel, Consumer<Integer> onPick) {
        JPopupMenu popup = new JPopupMenu();
        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JPanel top = new JPanel(new BorderLayout());
        JButton none = new JButton(noneLabel);
        none.addActionListener(e -> { popup.setVisible(false); onPick.accept(null); });
        top.add(none, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);
        JPanel themeGrid = new JPanel(new GridLayout(6, 10, 2, 2));
        int[] base = new int[10];
        for (int k = 0; k < 10; k++) base[k] = theme.color(k == 0 ? 1 : k == 1 ? 0 : k == 2 ? 3 : k == 3 ? 2 : k);
        double[] tints = {0, 0.8, 0.6, 0.4, -0.25, -0.5};
        for (double t : tints) for (int k = 0; k < 10; k++) themeGrid.add(swatch(t == 0 ? base[k] : SheetTheme.tint(base[k], t), popup, onPick));
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.add(new JLabel("Cores do tema"), BorderLayout.NORTH);
        center.add(themeGrid, BorderLayout.CENTER);
        JPanel standard = new JPanel(new GridLayout(1, 10, 2, 2));
        for (int c : STANDARD) standard.add(swatch(c, popup, onPick));
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.add(new JLabel("Cores padrão"), BorderLayout.NORTH);
        bottom.add(standard, BorderLayout.CENTER);
        JButton more = new JButton("Mais cores…");
        more.addActionListener(e -> {
            popup.setVisible(false);
            Color c = JColorChooser.showDialog(invoker, "Cores", Color.BLACK);
            if (c != null) onPick.accept(0xFF000000 | (c.getRGB() & 0xFFFFFF));
        });
        bottom.add(more, BorderLayout.SOUTH);
        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.add(center, BorderLayout.CENTER);
        stack.add(bottom, BorderLayout.SOUTH);
        root.add(stack, BorderLayout.CENTER);
        popup.add(root);
        popup.show(invoker, x, y);
    }

    private static JComponent swatch(int argb, JPopupMenu popup, Consumer<Integer> onPick) {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(16, 16));
        b.setBackground(new Color(argb, true));
        b.setOpaque(true);
        b.setBorder(BorderFactory.createLineBorder(UiTokens.border()));
        b.setToolTipText(String.format("#%06X", argb & 0xFFFFFF));
        b.addActionListener(e -> { popup.setVisible(false); onPick.accept(argb); });
        return b;
    }
}
