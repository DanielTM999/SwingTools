package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PathSearchTextField<T> extends SearchTextField<T> {

    public static final String PATH_SEGMENT_CLICK = "PATH_SEGMENT_CLICK";

    @Getter
    private String separator;

    @Getter
    private boolean editMode = false;

    private final JPanel breadcrumbPanel;

    @Getter
    @Setter
    private Color onMouseEnteredForegroundColor = null;

    @Getter
    @Setter
    private Color onMouseEnteredBackgroundColor = null;

    public PathSearchTextField(String separator) {
        super();
        this.separator = separator != null ? separator : "/";
        this.breadcrumbPanel = createBreadcrumbPanel();
        init();
    }

    public PathSearchTextField(String separator, int columns) {
        super(columns);
        this.separator = separator != null ? separator : "/";
        this.breadcrumbPanel = createBreadcrumbPanel();
        init();
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (!editMode) rebuildBreadcrumbs();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::rebuildBreadcrumbs);
    }

    @Override
    protected void registerValidEvents(Set<String> events) {
        super.registerValidEvents(events);
        events.add(PathTextField.PATH_SEGMENT_CLICK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!editMode) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            super.paintComponent(g);
        }
    }

    public void enterEditMode() {
        if (editMode || isReadonly()) return;
        editMode = true;
        breadcrumbPanel.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            selectAll();
        });
        repaint();
    }

    public void exitEditMode(boolean applyChanges) {
        if (!editMode) return;
        editMode = false;

        if (applyChanges) {
            rebuildBreadcrumbs();
            dispachEvent(EventType.CHANGE, getText());
        }

        breadcrumbPanel.setVisible(true);
        repaint();
    }

    public void setSeparator(String separator) {
        this.separator = separator != null ? separator : "/";
        rebuildBreadcrumbs();
    }

    public java.util.List<String> getSegments() {
        String path = getText();
        if (path == null || path.isEmpty()) return new ArrayList<>();
        String escapedSep = separator.equals("\\") ? "\\\\" : separator;
        return Arrays.stream(path.split(escapedSep, -1))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public java.util.List<String> getPathList() {
        java.util.List<String> segments = getSegments();
        java.util.List<String> paths = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) acc.append(separator);
            acc.append(segments.get(i));
            paths.add(acc.toString());
        }
        return paths;
    }

    private void init() {
        setLayout(new BorderLayout());
        setOpaque(true);

        add(breadcrumbPanel, BorderLayout.CENTER);
        breadcrumbPanel.setVisible(true);

        breadcrumbPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == breadcrumbPanel) enterEditMode();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (editMode) exitEditMode(true);
            }
        });

        addActionListener(e -> exitEditMode(true));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) exitEditMode(false);
            }
        });
    }

    private JPanel createBreadcrumbPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setCursor(Cursor.getPredefinedCursor(isReadonly() ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
        return panel;
    }

    private void rebuildBreadcrumbs() {
        breadcrumbPanel.removeAll();

        String path = getText();
        if (path == null || path.isEmpty()) {
            breadcrumbPanel.revalidate();
            breadcrumbPanel.repaint();
            return;
        }

        String escapedSep = separator.equals("\\") ? "\\\\" : separator;
        String[] parts = path.split(escapedSep, -1);

        List<String> accumulated = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) acc.append(separator);
            acc.append(parts[i]);
            accumulated.add(acc.toString());
        }

        for (int i = 0; i < parts.length; i++) {
            final String segLabel = parts[i];
            final String fullPath = accumulated.get(i);

            if (segLabel.isEmpty()) {
                if (i == 0) breadcrumbPanel.add(buildSeparatorLabel(separator));
                continue;
            }

            breadcrumbPanel.add(buildSegmentLabel(segLabel, fullPath));

            if (i < parts.length - 1) {
                breadcrumbPanel.add(buildSeparatorLabel(" › "));
            }
        }

        breadcrumbPanel.revalidate();
        breadcrumbPanel.repaint();
    }

    private JLabel buildSegmentLabel(String text, String fullPath) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(getFont());
        lbl.setForeground(getForeground());
        lbl.setBorder(new EmptyBorder(2, 4, 2, 4));
        lbl.setCursor(Cursor.getPredefinedCursor(isReadonly() ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        lbl.setOpaque(false);

        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lbl.setForeground(onMouseEnteredForegroundColor != null ? onMouseEnteredForegroundColor : getSelectionColor());
                lbl.setBackground(onMouseEnteredBackgroundColor != null ? onMouseEnteredBackgroundColor : getSelectionColor());
                lbl.setOpaque(true);
                lbl.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lbl.setForeground(getForeground());
                lbl.setOpaque(false);
                lbl.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dispachEvent(PATH_SEGMENT_CLICK, fullPath);
                e.consume();
            }
        });

        return lbl;
    }

    private JLabel buildSeparatorLabel(String text) {
        JLabel sep = new JLabel(text);
        sep.setFont(getFont().deriveFont(Font.BOLD));
        sep.setForeground(getDisabledTextColor());
        sep.setBorder(new EmptyBorder(2, 2, 2, 2));
        sep.setCursor(Cursor.getPredefinedCursor(isReadonly() ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));

        if (!isReadonly()) {
            sep.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    enterEditMode();
                }
            });
        }

        return sep;
    }

}
