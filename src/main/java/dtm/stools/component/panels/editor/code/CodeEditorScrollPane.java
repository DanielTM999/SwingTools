package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class CodeEditorScrollPane extends JScrollPane {

    @Getter
    private final CodeEditorGutter gutter;

    @Getter
    private CodeEditorMinimap minimap;

    @Getter
    private CodeEditorScrollBarUI verticalScrollBarUI;

    @Getter
    private CodeEditorScrollBarUI horizontalScrollBarUI;

    @Getter
    @Setter
    private boolean minimapEnabled = true;

    private MinimapScrollPaneLayout minimapLayout;

    public CodeEditorScrollPane(CodeEditorTextArea textArea) {
        this(textArea, null, null);
    }

    public CodeEditorScrollPane(CodeEditorTextArea textArea, CodeEditorGutter gutter) {
        this(textArea, gutter, null);
    }

    public CodeEditorScrollPane(CodeEditorTextArea textArea, CodeEditorGutter gutter, MinimapScrollPaneLayout minimapLayout) {
        super(textArea);
        this.verticalScrollBarUI = new CodeEditorScrollBarUI();
        this.horizontalScrollBarUI = new CodeEditorScrollBarUI();

        getVerticalScrollBar().setUI(verticalScrollBarUI);
        getHorizontalScrollBar().setUI(horizontalScrollBarUI);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.gutter = (gutter != null) ? gutter : new CodeEditorGutter(textArea);
        setRowHeaderView(this.gutter);
        getViewport().addChangeListener(e -> this.gutter.repaint());

        int lineHeight = textArea.getFontMetrics(textArea.getFont()).getHeight();
        getVerticalScrollBar().setUnitIncrement(lineHeight * 3);
        getHorizontalScrollBar().setUnitIncrement(lineHeight);

        this.minimap = new CodeEditorMinimap(textArea, this);
        this.minimapLayout = (minimapLayout != null) ? minimapLayout : new MinimapScrollPaneLayout();
        enableMinimap(true);
    }

    public void setMinimapLayout(MinimapScrollPaneLayout layout) {
        this.minimapLayout = layout;
        if (minimapEnabled) {
            enableMinimap(true);
        }
    }

    public void setVerticalScrollBarUI(CodeEditorScrollBarUI ui) {
        if (ui == null) return;

        this.verticalScrollBarUI = ui;
        getVerticalScrollBar().setUI(ui);
    }

    public void setHorizontalScrollBarUI(CodeEditorScrollBarUI ui) {
        if (ui == null) return;

        this.horizontalScrollBarUI = ui;
        getHorizontalScrollBar().setUI(ui);
    }

    public void setGoToPositionEnabled(boolean goToPositionEnabled){
        if(minimap != null) minimap.setGoToPositionEnabled(goToPositionEnabled);
    }

    public void enableMinimap(boolean enabled) {
        this.minimapEnabled = enabled;
        minimapLayout.setShowMinimap(enabled);
        minimapLayout.setMinimap(minimap);
        minimapLayout.setScrollPane(this);
        setLayout(minimapLayout);
        if (enabled) {
            add(minimap);
        }
        if (minimap != null) {
            minimap.refreshVisibilityState();
        }
        revalidate();
        doLayout();
        repaint();
    }

    public static class MinimapScrollPaneLayout extends ScrollPaneLayout {

        @Getter @Setter
        private boolean showMinimap;

        private CodeEditorMinimap minimap;
        private CodeEditorScrollPane scrollPane;

        void setMinimap(CodeEditorMinimap minimap) { this.minimap = minimap; }
        void setScrollPane(CodeEditorScrollPane scrollPane) { this.scrollPane = scrollPane; }

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            boolean masterEnabled = scrollPane != null && scrollPane.isMinimapEnabled();
            boolean isAlwaysMode = minimap != null && minimap.getVisibilityMode() == CodeEditorMinimap.VisibilityMode.ALWAYS;
            boolean hasScrollableContent = scrollPane != null
                    && scrollPane.getViewport() != null
                    && scrollPane.getViewport().getView() != null
                    && scrollPane.getViewport().getView().getPreferredSize().height
                        > scrollPane.getViewport().getHeight();

            if (!masterEnabled || minimap == null || !isAlwaysMode || !hasScrollableContent) {
                if (minimap != null) minimap.setVisible(false);
                return;
            }

            minimap.setVisible(true);
            int mw = minimap.getPreferredSize().width;
            Rectangle vpBounds = viewport.getBounds();
            int vsbWidth = (vsb != null && vsb.isVisible()) ? vsb.getWidth() : 0;

            viewport.setBounds(vpBounds.x, vpBounds.y, vpBounds.width - mw, vpBounds.height);

            if (rowHead != null && rowHead.isVisible()) {
                Rectangle rh = rowHead.getBounds();
                rowHead.setBounds(rh.x, rh.y, rh.width, vpBounds.height);
            }

            int vsbRight = (vsb != null && vsb.isVisible())
                    ? vsb.getBounds().x + vsb.getBounds().width
                    : vpBounds.x + vpBounds.width;

            if(vsb != null) vsb.setBounds(vsbRight - vsbWidth, vpBounds.y, vsbWidth, vpBounds.height);

            minimap.setBounds(
                    vsbRight - vsbWidth - mw,
                    vpBounds.y,
                    mw,
                    vpBounds.height
            );
        }

    }

    @Getter
    public static class CodeEditorScrollBarUI extends BasicScrollBarUI {

        private int minThumbSize = 40;
        private int thickness = 8;
        private int thumbArc = 8;
        private float thumbAlpha = 0.63f;
        private boolean trackVisible = false;

        public void setThickness(int thickness) {
            this.thickness = thickness;
            update();
        }

        public void setMinThumbSize(int minThumbSize) {
            this.minThumbSize = minThumbSize;
            update();
        }

        public void setThumbArc(int thumbArc) {
            this.thumbArc = thumbArc;
            repaint();
        }

        public void setThumbAlpha(float thumbAlpha) {
            this.thumbAlpha = thumbAlpha;
            repaint();
        }

        public void setTrackVisible(boolean trackVisible) {
            this.trackVisible = trackVisible;
            repaint();
        }

        private void update() {
            if (scrollbar != null) {
                scrollbar.revalidate();
                scrollbar.repaint();
            }
        }

        private void repaint() {
            if (scrollbar != null) {
                scrollbar.repaint();
            }
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            Dimension d = super.getMinimumThumbSize();

            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                d.height = Math.max(d.height, minThumbSize);
            } else {
                d.width = Math.max(d.width, minThumbSize);
            }

            return d;
        }

        @Override
        protected void setThumbBounds(int x, int y, int width, int height) {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                height = Math.max(height, minThumbSize);
                y = Math.min(y, trackRect.y + trackRect.height - height);
            } else {
                width = Math.max(width, minThumbSize);
                x = Math.min(x, trackRect.x + trackRect.width - width);
            }

            super.setThumbBounds(x, y, width, height);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                return new Dimension(thickness, super.getPreferredSize(c).height);
            } else {
                return new Dimension(super.getPreferredSize(c).width, thickness);
            }
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            if (!trackVisible) return;

            Color track = UIManager.getColor("ScrollBar.track");
            if (track != null) {
                g.setColor(track);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (!scrollbar.isEnabled()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = UIManager.getColor("ScrollBar.thumb");
            if (base == null) base = scrollbar.getForeground();

            int alpha = (int) (thumbAlpha * 255);
            Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

            g2.setColor(color);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, thumbArc, thumbArc);

            g2.dispose();
        }
    }

}