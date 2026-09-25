package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.model.SheetVisibility;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetContextMenuContext;
import dtm.stools.component.panels.editor.sheet.render.SheetPalette;
import dtm.stools.configs.UiTokens;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SheetTabBar extends JComponent {
    private record Tab(int index, Rectangle bounds) {}

    private final SheetEditor editor;
    private final List<Tab> tabs = new ArrayList<>();
    private int firstVisible;
    private int dragIndex = -1, dropIndex = -1;
    private JTextField renameField;
    private Rectangle prevButton, nextButton, addButton;

    public SheetTabBar(SheetEditor editor) {
        this.editor = editor;
        setName("sheet.tabs");
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        setToolTipText("");
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { press(e); }
            @Override public void mouseDragged(MouseEvent e) { if (dragIndex >= 0) { dropIndex = dropAt(e.getX()); repaint(); } }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) popup(e);
                if (dragIndex >= 0 && dropIndex >= 0 && dropIndex != dragIndex && dropIndex != dragIndex + 1) editor.moveSheet(dragIndex, dropIndex > dragIndex ? dropIndex - 1 : dropIndex);
                dragIndex = -1; dropIndex = -1; repaint();
            }
            @Override public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) { scrollTabs(e.getWheelRotation()); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(420, 30); }

    @Override public String getToolTipText(MouseEvent e) {
        if (addButton != null && addButton.contains(e.getPoint())) return "Nova planilha (Shift+F11)";
        Tab t = tabAt(e.getX(), e.getY());
        return t == null ? null : editor.getSession().getWorkbook().sheet(t.index()).name();
    }

    private void press(MouseEvent e) {
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) { popup(e); return; }
        if (prevButton != null && prevButton.contains(e.getPoint())) { scrollTabs(-1); return; }
        if (nextButton != null && nextButton.contains(e.getPoint())) { scrollTabs(1); return; }
        if (addButton != null && addButton.contains(e.getPoint())) { editor.execute("sheet.sheet.insert"); return; }
        Tab t = tabAt(e.getX(), e.getY());
        if (t == null) return;
        editor.activateSheet(t.index());
        if (e.getClickCount() == 2 && !editor.isReadOnlyView()) startRename(t);
        else dragIndex = t.index();
    }

    private void popup(MouseEvent e) {
        Tab t = tabAt(e.getX(), e.getY());
        if (t != null) editor.activateSheet(t.index());
        editor.showContextMenu(new SheetContextMenuContext(SheetContextMenuContext.Target.SHEET_TAB, editor.getSession().getActiveSheetIndex(), null, t == null ? -1 : t.index(), null), this, e.getX(), e.getY());
    }

    private void scrollTabs(int delta) {
        firstVisible = Math.max(0, Math.min(editor.getSession().getWorkbook().sheetCount() - 1, firstVisible + delta));
        repaint();
    }

    public void ensureVisible(int index) {
        if (index < firstVisible) firstVisible = index;
        else {
            boolean visible = false;
            for (Tab t : tabs) if (t.index() == index && t.bounds().x + t.bounds().width <= getWidth() - 40) visible = true;
            if (!visible && !tabs.isEmpty() && tabs.stream().noneMatch(t -> t.index() == index)) firstVisible = Math.max(0, index - 2);
        }
        repaint();
    }

    public void startRename(int index) {
        for (Tab t : tabs) if (t.index() == index) { startRename(t); return; }
    }

    private void startRename(Tab t) {
        if (renameField != null) remove(renameField);
        SheetWorksheet ws = editor.getSession().getWorkbook().sheet(t.index());
        renameField = new JTextField(ws.name());
        renameField.setBounds(t.bounds().x, t.bounds().y + 2, Math.max(90, t.bounds().width), t.bounds().height - 4);
        add(renameField);
        renameField.selectAll();
        renameField.requestFocusInWindow();
        Runnable finish = () -> {
            if (renameField == null) return;
            String name = renameField.getText().strip();
            JTextField f = renameField;
            renameField = null;
            remove(f);
            repaint();
            if (!name.equals(ws.name())) editor.renameSheet(t.index(), name);
            editor.getCanvas().requestFocusInWindow();
        };
        renameField.addActionListener(e -> finish.run());
        renameField.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { finish.run(); } });
        renameField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { JTextField f = renameField; renameField = null; remove(f); repaint(); editor.getCanvas().requestFocusInWindow(); }
            }
        });
        repaint();
    }

    private Tab tabAt(int x, int y) {
        for (Tab t : tabs) if (t.bounds().contains(x, y)) return t;
        return null;
    }

    private int dropAt(int x) {
        for (Tab t : tabs) if (x < t.bounds().getCenterX()) return t.index();
        return tabs.isEmpty() ? 0 : tabs.getLast().index() + 1;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean dark = UiTokens.isDarkTheme();
            g.setColor(dark ? new Color(0x252525) : new Color(0xF3F3F3));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(UiTokens.border());
            g.drawLine(0, 0, getWidth(), 0);
            FontMetrics fm = g.getFontMetrics(getFont());
            g.setFont(getFont());
            prevButton = new Rectangle(4, 5, 20, 20);
            nextButton = new Rectangle(24, 5, 20, 20);
            Color fg = dark ? new Color(0xDDDDDD) : new Color(0x444444);
            g.setColor(firstVisible > 0 ? fg : new Color(0xAAAAAA));
            g.fillPolygon(new int[]{17, 11, 17}, new int[]{10, 15, 20}, 3);
            g.setColor(fg);
            g.fillPolygon(new int[]{31, 37, 31}, new int[]{10, 15, 20}, 3);
            tabs.clear();
            SheetWorkbook wb = editor.getSession().getWorkbook();
            int active = editor.getSession().getActiveSheetIndex();
            int x = 50;
            for (int k = firstVisible; k < wb.sheetCount(); k++) {
                SheetWorksheet ws = wb.sheet(k);
                if (ws.properties().visibility() != SheetVisibility.VISIBLE) continue;
                String name = ws.name();
                int w = Math.min(220, fm.stringWidth(name) + 28);
                if (x + w > getWidth() - 36) break;
                Rectangle r = new Rectangle(x, 0, w, getHeight() - 3);
                tabs.add(new Tab(k, r));
                boolean isActive = k == active;
                if (isActive) {
                    g.setColor(dark ? new Color(0x1F1F1F) : Color.WHITE);
                    g.fillRect(r.x, r.y, r.width, r.height);
                    g.setColor(UiTokens.border());
                    g.drawLine(r.x, r.y, r.x, r.y + r.height);
                    g.drawLine(r.x + r.width, r.y, r.x + r.width, r.y + r.height);
                    g.setColor(new Color(0x107C41));
                    g.setStroke(new BasicStroke(2.5f));
                    g.drawLine(r.x + 6, r.y + r.height - 2, r.x + r.width - 6, r.y + r.height - 2);
                    g.setStroke(new BasicStroke(1f));
                } else {
                    g.setColor(UiTokens.border());
                    g.drawLine(r.x + r.width, r.y + 6, r.x + r.width, r.y + r.height - 6);
                }
                if (ws.properties().tabColor() != null) {
                    g.setColor(SheetPalette.color(ws.properties().tabColor()));
                    if (isActive) g.fillRect(r.x + 1, r.y, r.width - 1, 3); else g.fillRect(r.x + 1, r.y + r.height - 4, r.width - 2, 4);
                }
                g.setColor(isActive ? (dark ? Color.WHITE : new Color(0x107C41)) : fg);
                g.setFont(isActive ? getFont().deriveFont(Font.BOLD) : getFont());
                String t = name;
                FontMetrics m = g.getFontMetrics();
                while (m.stringWidth(t) > w - 20 && t.length() > 1) t = t.substring(0, t.length() - 1);
                if (!t.equals(name)) t += "…";
                g.drawString(t, r.x + (w - m.stringWidth(t)) / 2, r.y + (r.height + m.getAscent() - m.getDescent()) / 2);
                if (dropIndex == k) { g.setColor(new Color(0x107C41)); g.fillRect(r.x - 1, 2, 3, r.height - 4); }
                x += w;
            }
            if (dropIndex >= 0 && !tabs.isEmpty() && dropIndex > tabs.getLast().index()) { g.setColor(new Color(0x107C41)); g.fillRect(x - 1, 2, 3, getHeight() - 7); }
            addButton = new Rectangle(x + 6, 5, 22, 20);
            if (!editor.isReadOnlyView()) {
                g.setColor(fg);
                g.setStroke(new BasicStroke(1.6f));
                int cx = addButton.x + 11, cy = addButton.y + 10;
                g.drawOval(addButton.x + 2, addButton.y + 1, 18, 18);
                g.drawLine(cx - 4, cy, cx + 4, cy);
                g.drawLine(cx, cy - 4, cx, cy + 4);
            }
        } finally {
            g.dispose();
        }
    }
}
