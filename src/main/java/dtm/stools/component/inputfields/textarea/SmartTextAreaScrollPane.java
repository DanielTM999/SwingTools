package dtm.stools.component.inputfields.textarea;

import dtm.stools.configs.UiTokens;
import dtm.stools.models.SearchPanelMode;
import lombok.Getter;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

public class SmartTextAreaScrollPane extends JScrollPane {

    private static final String SEARCH_BAR = "smartTextArea.searchBar";
    private static final String RESIZE_GRIP = "smartTextArea.resizeGrip";

    @Getter
    private final SmartTextArea textArea;
    private final TextAreaLineGutter gutter;
    private final TextAreaSearchBar searchBar;
    private final TextAreaResizeGrip resizeGrip;

    public SmartTextAreaScrollPane(SmartTextArea textArea) {
        super(textArea);
        this.textArea = textArea;
        this.gutter = new TextAreaLineGutter(textArea);
        this.searchBar = new TextAreaSearchBar(textArea);
        setLayout(new SmartScrollPaneLayout());
        this.resizeGrip = new TextAreaResizeGrip(this);
        add(resizeGrip, RESIZE_GRIP, 0);
        add(searchBar, SEARCH_BAR, 0);
        textArea.addPropertyChangeListener("lineNumbersVisible", e -> syncGutter());
        textArea.addPropertyChangeListener("searchMode", e -> {
            searchBar.setFloating(isFloatingSearch());
            revalidate();
            repaint();
        });
        searchBar.setFloating(isFloatingSearch());
        textArea.addPropertyChangeListener("resize", e -> {
            resizeGrip.syncCursor();
            revalidate();
            repaint();
        });
        syncGutter();
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        boolean overlaySearch = searchBar != null && searchBar.isVisible() && isFloatingSearch();
        return !overlaySearch && (resizeGrip == null || !resizeGrip.isVisible());
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        int reserved = embeddedSearchHeight();
        if (reserved > 0) insets = new Insets(insets.top + reserved, insets.left, insets.bottom, insets.right);
        return insets;
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets resolved = getInsets();
        if (insets == null) return resolved;
        insets.set(resolved.top, resolved.left, resolved.bottom, resolved.right);
        return insets;
    }

    private boolean isFloatingSearch() {
        return textArea == null || textArea.getSearchMode() != SearchPanelMode.EMBEDDED;
    }

    private int embeddedSearchHeight() {
        if (searchBar == null || !searchBar.isVisible() || isFloatingSearch()) return 0;
        return searchBar.getPreferredSize().height;
    }

    private Insets borderInsets() {
        return super.getInsets();
    }

    TextAreaSearchBar getSearchBar() {
        return searchBar;
    }

    TextAreaResizeGrip getResizeGrip() {
        return resizeGrip;
    }

    private void syncGutter() {
        setRowHeaderView(textArea.isLineNumbersVisible() ? gutter : null);
        revalidate();
        repaint();
    }

    private final class SmartScrollPaneLayout extends ScrollPaneLayout {

        private Component searchOverlay;
        private Component gripOverlay;

        @Override
        public void addLayoutComponent(String key, Component component) {
            if (SEARCH_BAR.equals(key)) {
                searchOverlay = component;
                return;
            }
            if (RESIZE_GRIP.equals(key)) {
                gripOverlay = component;
                return;
            }
            super.addLayoutComponent(key, component);
        }

        @Override
        public void removeLayoutComponent(Component component) {
            if (component == searchOverlay) {
                searchOverlay = null;
                return;
            }
            if (component == gripOverlay) {
                gripOverlay = null;
                return;
            }
            super.removeLayoutComponent(component);
        }

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            layoutGrip(parent);
            layoutSearch(parent);
        }

        private void layoutGrip(Container parent) {
            if (gripOverlay == null) return;
            if (!gripOverlay.isVisible()) {
                gripOverlay.setBounds(0, 0, 0, 0);
                return;
            }
            int size = gripOverlay.getPreferredSize().width;
            Insets insets = parent.getInsets();
            int x = parent.getWidth() - insets.right - size;
            int y = parent.getHeight() - insets.bottom - size;
            if (vsb != null && vsb.isVisible()) {
                Rectangle bounds = vsb.getBounds();
                vsb.setBounds(bounds.x, bounds.y, bounds.width, Math.max(0, Math.min(bounds.height, y - bounds.y)));
            }
            if (hsb != null && hsb.isVisible()) {
                Rectangle bounds = hsb.getBounds();
                hsb.setBounds(bounds.x, bounds.y, Math.max(0, Math.min(bounds.width, x - bounds.x)), bounds.height);
            }
            gripOverlay.setBounds(x, y, size, size);
        }

        private void layoutSearch(Container parent) {
            if (searchOverlay == null) return;
            if (!searchOverlay.isVisible() || viewport == null) {
                searchOverlay.setBounds(0, 0, 0, 0);
                return;
            }
            if (!isFloatingSearch()) {
                Insets border = borderInsets();
                int width = Math.max(0, parent.getWidth() - border.left - border.right);
                searchOverlay.setBounds(border.left, border.top, width, embeddedSearchHeight());
                return;
            }
            Rectangle area = viewport.getBounds();
            int margin = UiTokens.scale(6);
            Dimension preferred = searchOverlay.getPreferredSize();
            int width = Math.max(0, Math.min(preferred.width, area.width - margin * 2));
            int height = Math.min(preferred.height, Math.max(0, area.height - margin * 2));
            searchOverlay.setBounds(area.x + area.width - width - margin, area.y + margin, width, height);
        }
    }
}
