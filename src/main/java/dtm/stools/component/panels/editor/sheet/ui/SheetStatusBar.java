package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class SheetStatusBar extends JPanel {
    private final SheetEditor editor;
    private final JLabel mode = new JLabel("Pronto");
    private final JLabel info = new JLabel();
    private final JLabel aggregates = new JLabel();
    private final JLabel zoomLabel = new JLabel("100%");
    private final JSlider zoom = new JSlider(10, 400, 100);
    private final JToggleButton normal, layout, breaks;
    private final Map<String, Boolean> shown = new LinkedHashMap<>();
    private boolean updating;

    public SheetStatusBar(SheetEditor editor) {
        super(new BorderLayout(8, 0));
        this.editor = editor;
        setName("sheet.status");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTokens.border()), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        for (String k : new String[]{"Média", "Contagem", "Contagem Numérica", "Mín", "Máx", "Soma"}) shown.put(k, !k.equals("Contagem Numérica") && !k.equals("Mín") && !k.equals("Máx"));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(mode);
        left.add(Box.createHorizontalStrut(16));
        left.add(info);
        add(left, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(aggregates);
        right.add(Box.createHorizontalStrut(16));
        normal = view("normal-view", "Normal", SheetViewMode.NORMAL);
        layout = view("page-layout", "Layout da Página", SheetViewMode.PAGE_LAYOUT);
        breaks = view("page-break", "Visualização da Quebra de Página", SheetViewMode.PAGE_BREAK_PREVIEW);
        right.add(normal); right.add(layout); right.add(breaks);
        right.add(Box.createHorizontalStrut(10));
        JButton minus = small("−", () -> editor.setZoom(Math.max(0.1, editor.effectiveZoom() - 0.1)));
        JButton plus = small("+", () -> editor.setZoom(Math.min(4, editor.effectiveZoom() + 0.1)));
        zoom.setPreferredSize(new Dimension(110, 20));
        zoom.setMaximumSize(new Dimension(110, 22));
        zoom.setOpaque(false);
        zoom.addChangeListener(e -> { if (!updating) editor.setZoom(zoom.getValue() / 100.0); });
        zoomLabel.setPreferredSize(new Dimension(44, 20));
        zoomLabel.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { editor.execute("sheet.view.zoomDialog"); } });
        right.add(minus); right.add(zoom); right.add(plus); right.add(zoomLabel);
        add(right, BorderLayout.EAST);
        aggregates.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) menu(e); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) menu(e); }
        });
        aggregates.setToolTipText("Clique com o botão direito para escolher os valores exibidos");
    }

    private void menu(MouseEvent e) {
        JPopupMenu m = new JPopupMenu();
        for (String k : shown.keySet()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(k, shown.get(k));
            item.addActionListener(a -> { shown.put(k, item.isSelected()); editor.refreshStatus(); });
            m.add(item);
        }
        m.show(aggregates, e.getX(), e.getY());
    }

    private JToggleButton view(String icon, String tip, SheetViewMode mode) {
        JToggleButton b = new JToggleButton(SheetIcon.small(icon));
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.addActionListener(e -> editor.setViewMode(mode));
        return b;
    }

    private static JButton small(String text, Runnable r) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setMargin(new java.awt.Insets(0, 4, 0, 4));
        b.addActionListener(e -> r.run());
        return b;
    }

    public Map<String, Boolean> shownAggregates() { return shown; }

    public void update(String modeText, String infoText, String aggregatesText, double zoomValue, SheetViewMode viewMode) {
        updating = true;
        try {
            mode.setText(modeText);
            info.setText(infoText == null ? "" : infoText);
            aggregates.setText(aggregatesText == null ? "" : aggregatesText);
            zoom.setValue((int) Math.round(zoomValue * 100));
            zoomLabel.setText(Math.round(zoomValue * 100) + "%");
            normal.setSelected(viewMode == SheetViewMode.NORMAL);
            layout.setSelected(viewMode == SheetViewMode.PAGE_LAYOUT);
            breaks.setSelected(viewMode == SheetViewMode.PAGE_BREAK_PREVIEW);
        } finally {
            updating = false;
        }
    }
}
