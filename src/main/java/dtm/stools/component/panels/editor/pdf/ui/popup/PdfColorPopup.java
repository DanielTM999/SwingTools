package dtm.stools.component.panels.editor.pdf.ui.popup;

import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.function.Consumer;

public final class PdfColorPopup {
    private static final int[] PALETTE = {
            0x000000, 0x404040, 0x7F7F7F, 0xBFBFBF, 0xFFFFFF, 0xC00000, 0xFF0000, 0xFFC000,
            0xFFFF00, 0x92D050, 0x00B050, 0x00B0F0, 0x0070C0, 0x1F6FD1, 0x002060, 0x7030A0,
            0xF4B6C2, 0xFFE38A, 0xC6EFCE, 0xDDEBF7, 0xE2D6F3, 0x8B5A2B, 0xFF7F50, 0x2E8B57};

    private PdfColorPopup() {}

    public static void show(Component owner, int x, int y, String noneLabel, Consumer<Color> selected) {
        JPopupMenu popup = new JPopupMenu();
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JPanel grid = new JPanel(new GridLayout(3, 8, 3, 3));
        for (int rgb : PALETTE) {
            Color color = new Color(rgb);
            JButton swatch = swatch(color);
            swatch.addActionListener(event -> { popup.setVisible(false); selected.accept(color); });
            grid.add(swatch);
        }
        content.add(grid, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 2));
        if (noneLabel != null) {
            JButton none = new JButton(noneLabel);
            none.addActionListener(event -> { popup.setVisible(false); selected.accept(null); });
            actions.add(none);
        }
        JButton more = new JButton("Mais cores…");
        more.addActionListener(event -> {
            popup.setVisible(false);
            Color chosen = JColorChooser.showDialog(owner, "Escolher cor", Color.BLACK);
            if (chosen != null) selected.accept(chosen);
        });
        actions.add(more);
        content.add(actions, BorderLayout.SOUTH);
        popup.add(content);
        popup.show(owner, x, y);
    }

    private static JButton swatch(Color color) {
        JButton button = new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                graphics.setColor(color);
                graphics.fillRect(0, 0, getWidth(), getHeight());
                graphics.setColor(getModel().isRollover() ? UiTokens.accent() : UiTokens.border());
                graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        button.setPreferredSize(new Dimension(18, 18));
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setToolTipText(String.format("#%06X", color.getRGB() & 0xFFFFFF));
        ((JComponent) button).putClientProperty("JButton.buttonType", "toolBarButton");
        return button;
    }
}
