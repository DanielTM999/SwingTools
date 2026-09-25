package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

final class SheetRibbonPage extends JPanel {
    enum Mode { FULL, COMPACT, COLLAPSED, OVERFLOW }

    final class GroupView {
        final RibbonGroup group;
        final JComponent full, compact;
        final JButton collapsed;
        Mode mode = Mode.FULL;

        GroupView(RibbonGroup group, JComponent full, JComponent compact) {
            this.group = group;
            this.full = full;
            this.compact = compact;
            collapsed = new JButton(group.title(), SheetIcon.large(group.icon()));
            collapsed.setVerticalTextPosition(SwingConstants.BOTTOM);
            collapsed.setHorizontalTextPosition(SwingConstants.CENTER);
            collapsed.putClientProperty("JButton.buttonType", "toolBarButton");
            collapsed.setFocusable(false);
            collapsed.addActionListener(e -> {
                JPopupMenu popup = new JPopupMenu();
                popup.add(builder.apply(group));
                popup.show(collapsed, 0, collapsed.getHeight());
            });
        }

        JComponent current() { return mode == Mode.COMPACT ? compact : mode == Mode.COLLAPSED ? collapsed : full; }
    }

    private final List<GroupView> views = new ArrayList<>();
    private final JButton more = new JButton("Mais", SheetIcon.large("more"));
    private final Function<RibbonGroup, JComponent> builder;

    SheetRibbonPage(List<RibbonGroup> groups, Function<RibbonGroup, JComponent> fullBuilder, Function<RibbonGroup, JComponent> compactBuilder) {
        this.builder = fullBuilder;
        setLayout(new PageLayout());
        setOpaque(false);
        for (RibbonGroup g : groups) views.add(new GroupView(g, frame(g, fullBuilder.apply(g)), frame(g, compactBuilder.apply(g))));
        more.setVerticalTextPosition(SwingConstants.BOTTOM);
        more.setHorizontalTextPosition(SwingConstants.CENTER);
        more.putClientProperty("JButton.buttonType", "toolBarButton");
        more.setFocusable(false);
        more.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            for (GroupView v : views) if (v.mode == Mode.OVERFLOW) panel.add(frame(v.group, builder.apply(v.group)));
            popup.add(panel);
            popup.show(more, 0, more.getHeight());
        });
        rebuild();
    }

    List<GroupView> views() { return views; }

    private JComponent frame(RibbonGroup g, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTokens.border()), BorderFactory.createEmptyBorder(2, 4, 0, 6)));
        panel.add(content, BorderLayout.CENTER);
        JLabel title = new JLabel(g.title(), SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() - 1f));
        title.setForeground(UiTokens.muted());
        panel.add(title, BorderLayout.SOUTH);
        panel.setName("sheet.ribbon.group." + g.id());
        return panel;
    }

    private void rebuild() {
        removeAll();
        for (GroupView v : views) if (v.mode != Mode.OVERFLOW) add(v.current());
        if (views.stream().anyMatch(v -> v.mode == Mode.OVERFLOW)) add(more);
    }

    void adapt(int width) {
        for (GroupView v : views) v.mode = Mode.FULL;
        List<GroupView> order = new ArrayList<>(views);
        order.sort(Comparator.comparingInt(v -> v.group.priority()));
        while (required() > width) {
            GroupView next = null;
            for (GroupView v : order) if (v.mode == Mode.FULL) { next = v; break; }
            if (next != null) { next.mode = Mode.COMPACT; if (required() <= width) break; continue; }
            for (GroupView v : order) if (v.mode == Mode.COMPACT) { next = v; break; }
            if (next != null) { next.mode = Mode.COLLAPSED; continue; }
            for (GroupView v : order) if (v.mode == Mode.COLLAPSED) { next = v; break; }
            if (next == null) break;
            next.mode = Mode.OVERFLOW;
        }
        rebuild();
        revalidate();
        repaint();
    }

    private int required() {
        int w = 0;
        boolean overflow = false;
        for (GroupView v : views) {
            if (v.mode == Mode.OVERFLOW) { overflow = true; continue; }
            w += v.current().getPreferredSize().width + 2;
        }
        if (overflow) w += more.getPreferredSize().width + 4;
        return w;
    }

    private static final class PageLayout implements LayoutManager {
        @Override public void addLayoutComponent(String name, Component comp) { }
        @Override public void removeLayoutComponent(Component comp) { }
        @Override public Dimension preferredLayoutSize(Container parent) {
            int w = 0, h = 0;
            for (Component c : parent.getComponents()) { Dimension d = c.getPreferredSize(); w += d.width + 2; h = Math.max(h, d.height); }
            Insets in = parent.getInsets();
            return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
        }
        @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(80, preferredLayoutSize(parent).height); }
        @Override public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int x = in.left, h = parent.getHeight() - in.top - in.bottom;
            for (Component c : parent.getComponents()) {
                Dimension d = c.getPreferredSize();
                c.setBounds(x, in.top, d.width, h);
                x += d.width + 2;
            }
        }
    }
}
