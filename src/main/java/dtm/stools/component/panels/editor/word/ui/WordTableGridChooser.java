package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public final class WordTableGridChooser extends JComponent {
    private static final int CELL = 18, COLUMNS = 10, ROWS = 8;
    private int rows = 1, columns = 1;

    public WordTableGridChooser(JPopupMenu owner, BiConsumer<Integer,Integer> chosen) {
        setPreferredSize(new Dimension(COLUMNS*CELL+8,ROWS*CELL+26));
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                columns = Math.max(1,Math.min(COLUMNS,(e.getX()-4)/CELL+1)); rows = Math.max(1,Math.min(ROWS,(e.getY()-4)/CELL+1)); repaint();
            }
            @Override public void mouseClicked(MouseEvent e) { owner.setVisible(false); chosen.accept(rows,columns); }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        getAccessibleContext().setAccessibleName("Escolher tamanho da tabela");
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        try {
            for (int r = 0; r < ROWS; r++) for (int c = 0; c < COLUMNS; c++) {
                boolean on = r < rows && c < columns;
                g2.setColor(on ? UiTokens.accent() : UiTokens.surface()); g2.fillRect(4+c*CELL,4+r*CELL,CELL-3,CELL-3);
                g2.setColor(UiTokens.border()); g2.drawRect(4+c*CELL,4+r*CELL,CELL-3,CELL-3);
            }
            g2.setColor(UiTokens.foreground()); g2.setFont(UiTokens.fontSmall());
            g2.drawString("Tabela " + columns + " × " + rows,6,ROWS*CELL+20);
        } finally { g2.dispose(); }
    }
    public static JPopupMenu popup(BiConsumer<Integer,Integer> chosen) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new WordTableGridChooser(menu,chosen));
        return menu;
    }
}
