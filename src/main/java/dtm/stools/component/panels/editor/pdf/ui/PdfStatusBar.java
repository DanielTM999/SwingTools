package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PdfStatusBar extends JPanel {
    private final PdfEditor editor;
    private final JLabel page = new JLabel();
    private final JLabel tool = new JLabel();
    private final JLabel info = new JLabel();
    private final JLabel dirty = new JLabel();
    private final JLabel zoomLabel = new JLabel("100%");
    private final JSlider zoom = new JSlider(10, 800, 100);
    private boolean updating;

    public PdfStatusBar(PdfEditor editor) {
        super(new BorderLayout(8, 0));
        this.editor = editor;
        setName("pdf.status");
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        page.setFont(UiTokens.fontSmall());
        tool.setFont(UiTokens.fontSmall());
        info.setFont(UiTokens.fontSmall());
        dirty.setFont(UiTokens.fontSmall());
        left.add(page);
        left.add(Box.createHorizontalStrut(16));
        left.add(tool);
        left.add(Box.createHorizontalStrut(16));
        left.add(info);
        left.add(Box.createHorizontalStrut(16));
        left.add(dirty);
        add(left, BorderLayout.CENTER);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        JButton out = button("zoom-out", "Diminuir zoom", "pdf.zoomOut");
        JButton in = button("zoom-in", "Aumentar zoom", "pdf.zoomIn");
        zoom.setPreferredSize(new Dimension(120, 20));
        zoom.setMaximumSize(new Dimension(120, 20));
        zoom.setFocusable(false);
        zoom.setName("pdf.status.zoom");
        zoom.addChangeListener(event -> {
            if (!updating && !zoom.getValueIsAdjusting()) editor.setZoom(zoom.getValue() / 100.0);
            zoomLabel.setText(zoom.getValue() + "%");
        });
        zoomLabel.setFont(UiTokens.fontSmall());
        zoomLabel.setPreferredSize(new Dimension(44, 18));
        zoomLabel.setHorizontalAlignment(JLabel.RIGHT);
        zoomLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        zoomLabel.setToolTipText("Escolher zoom");
        zoomLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) { zoomMenu(); }
        });
        right.add(out);
        right.add(zoom);
        right.add(in);
        right.add(Box.createHorizontalStrut(4));
        right.add(zoomLabel);
        add(right, BorderLayout.EAST);
        applyTheme();
    }

    public void applyTheme() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTokens.border()),
                BorderFactory.createEmptyBorder(3, 10, 3, 8)));
        tool.setForeground(UiTokens.muted());
        info.setForeground(UiTokens.muted());
        dirty.setForeground(UiTokens.warning());
    }

    private JButton button(String icon, String tip, String command) {
        JButton button = new JButton(PdfIcon.small(icon));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
        button.setToolTipText(tip);
        button.addActionListener(event -> editor.execute(command));
        return button;
    }

    private void zoomMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (int value : new int[]{50, 75, 100, 125, 150, 200, 300, 400}) {
            JMenuItem item = new JMenuItem(value + "%");
            item.addActionListener(event -> editor.setZoom(value / 100.0));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem width = new JMenuItem("Ajustar à largura");
        width.addActionListener(event -> editor.execute("pdf.fitWidth"));
        menu.add(width);
        JMenuItem whole = new JMenuItem("Página inteira");
        whole.addActionListener(event -> editor.execute("pdf.fitPage"));
        menu.add(whole);
        menu.show(zoomLabel, 0, -menu.getPreferredSize().height);
    }

    public void update(int current, int count, String toolName, String selection, boolean changed, double zoomValue) {
        updating = true;
        try {
            page.setText(count == 0 ? "" : "Página " + (current + 1) + " de " + count);
            tool.setText(toolName == null ? "" : toolName);
            info.setText(selection == null ? "" : selection);
            dirty.setText(changed ? "● Alterado" : "");
            int percent = (int) Math.round(zoomValue * 100);
            if (zoom.getValue() != percent) zoom.setValue(Math.max(zoom.getMinimum(), Math.min(zoom.getMaximum(), percent)));
            zoomLabel.setText(percent + "%");
        } finally { updating = false; }
    }

    public void message(String text) {
        info.setText(text);
    }
}
