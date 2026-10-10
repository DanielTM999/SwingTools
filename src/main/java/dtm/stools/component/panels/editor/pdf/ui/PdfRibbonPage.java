package dtm.stools.component.panels.editor.pdf.ui;

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

final class PdfRibbonPage extends JPanel {
    private final List<PdfRibbonGroupView> views = new ArrayList<>();
    private final JButton more = new JButton("Mais", PdfIcon.large("more"));
    private final Function<PdfRibbonGroup, JComponent> builder;

    PdfRibbonPage(List<PdfRibbonGroup> groups, Function<PdfRibbonGroup, JComponent> fullBuilder,
                  Function<PdfRibbonGroup, JComponent> compactBuilder) {
        this.builder = fullBuilder;
        setLayout(new PageLayout());
        setOpaque(false);
        for (PdfRibbonGroup group : groups)
            views.add(new PdfRibbonGroupView(group, frame(group, fullBuilder.apply(group)),
                    frame(group, compactBuilder.apply(group)), collapsed(group)));
        configure(more);
        more.addActionListener(event -> {
            JPopupMenu popup = new JPopupMenu();
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            for (PdfRibbonGroupView view : views) if (view.mode == PdfRibbonMode.OVERFLOW) panel.add(frame(view.group, builder.apply(view.group)));
            popup.add(panel);
            popup.show(more, 0, more.getHeight());
        });
        rebuild();
    }

    private JButton collapsed(PdfRibbonGroup group) {
        JButton button = new JButton(group.title(), PdfIcon.large(group.icon()));
        configure(button);
        button.setName("pdf.ribbon.collapsed." + group.id());
        button.addActionListener(event -> {
            JPopupMenu popup = new JPopupMenu();
            popup.add(builder.apply(group));
            popup.show(button, 0, button.getHeight());
        });
        return button;
    }

    private static void configure(JButton button) {
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
    }

    private JComponent frame(PdfRibbonGroup group, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTokens.border()),
                BorderFactory.createEmptyBorder(2, 5, 0, 7)));
        panel.add(content, BorderLayout.CENTER);
        JLabel title = new JLabel(group.title(), SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() - 1f));
        title.setForeground(UiTokens.muted());
        title.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        panel.add(title, BorderLayout.SOUTH);
        panel.setName("pdf.ribbon.group." + group.id());
        return panel;
    }

    private void rebuild() {
        removeAll();
        for (PdfRibbonGroupView view : views) if (view.mode != PdfRibbonMode.OVERFLOW) add(view.current());
        if (views.stream().anyMatch(view -> view.mode == PdfRibbonMode.OVERFLOW)) add(more);
    }

    void adapt(int width) {
        for (PdfRibbonGroupView view : views) view.mode = PdfRibbonMode.FULL;
        List<PdfRibbonGroupView> order = new ArrayList<>(views);
        order.sort(Comparator.comparingInt(view -> view.group.priority()));
        while (required() > width) {
            PdfRibbonGroupView next = first(order, PdfRibbonMode.FULL);
            if (next != null) { next.mode = PdfRibbonMode.COMPACT; continue; }
            next = first(order, PdfRibbonMode.COMPACT);
            if (next != null) { next.mode = PdfRibbonMode.COLLAPSED; continue; }
            next = first(order, PdfRibbonMode.COLLAPSED);
            if (next == null) break;
            next.mode = PdfRibbonMode.OVERFLOW;
        }
        rebuild();
        revalidate();
        repaint();
    }

    private static PdfRibbonGroupView first(List<PdfRibbonGroupView> order, PdfRibbonMode mode) {
        for (PdfRibbonGroupView view : order) if (view.mode == mode) return view;
        return null;
    }

    private int required() {
        int width = 0;
        boolean overflow = false;
        for (PdfRibbonGroupView view : views) {
            if (view.mode == PdfRibbonMode.OVERFLOW) { overflow = true; continue; }
            width += view.current().getPreferredSize().width + 2;
        }
        return overflow ? width + more.getPreferredSize().width + 4 : width;
    }

    private static final class PageLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component component) { }
        @Override
        public void removeLayoutComponent(Component component) { }
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int width = 0, height = 0;
            for (Component child : parent.getComponents()) {
                Dimension size = child.getPreferredSize();
                width += size.width + 2;
                height = Math.max(height, size.height);
            }
            Insets insets = parent.getInsets();
            return new Dimension(width + insets.left + insets.right, Math.max(86, height) + insets.top + insets.bottom);
        }
        @Override
        public Dimension minimumLayoutSize(Container parent) { return new Dimension(80, preferredLayoutSize(parent).height); }
        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int x = insets.left, height = parent.getHeight() - insets.top - insets.bottom;
            for (Component child : parent.getComponents()) {
                Dimension size = child.getPreferredSize();
                child.setBounds(x, insets.top, size.width, height);
                x += size.width + 2;
            }
        }
    }
}
