package dtm.stools.layouts;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

public class FlexBoxLayout implements LayoutManager2 {

    public enum Direction { ROW, COLUMN }
    public enum Justify   { START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }
    public enum Align     { START, CENTER, END, STRETCH }

    public static class FlexConstraints {

        private int grow          = 0;
        private int basis         = -1;
        private double widthPercent  = -1;
        private double heightPercent = -1;
        private int fixedWidth    = -1;
        private int fixedHeight   = -1;
        private int minWidth      = -1;
        private int maxWidth      = -1;
        private int minHeight     = -1;
        private int maxHeight     = -1;
        private Align alignSelf     = null;

        private FlexConstraints() {}

        public static FlexConstraints of() {
            return new FlexConstraints();
        }

        public FlexConstraints grow(int value) {
            this.grow = value;
            return this;
        }

        public FlexConstraints basis(int value) {
            this.basis = value;
            return this;
        }

        public FlexConstraints widthPercent(double v) {
            this.widthPercent = v;
            return this;
        }

        public FlexConstraints heightPercent(double v) {
            this.heightPercent = v;
            return this;
        }

        public FlexConstraints fixedWidth(int px) {
            this.fixedWidth = px;
            return this;
        }

        public FlexConstraints fixedHeight(int px) {
            this.fixedHeight = px;
            return this;
        }

        public FlexConstraints minWidth(int px) {
            this.minWidth = px;
            return this;
        }

        public FlexConstraints maxWidth(int px) {
            this.maxWidth = px;
            return this;
        }

        public FlexConstraints minHeight(int px) {
            this.minHeight = px;
            return this;
        }

        public FlexConstraints maxHeight(int px) {
            this.maxHeight = px;
            return this;
        }

        public FlexConstraints alignSelf(Align align) {
            this.alignSelf = align;
            return this;
        }
    }

    public static class Builder {

        Direction direction = Direction.ROW;
        Justify justify = Justify.START;
        Align align = Align.START;
        boolean wrap = false;
        boolean reverse = false;
        int hgap = 0;
        int vgap = 0;
        int paddingTop = 0;
        int paddingBottom = 0;
        int paddingLeft = 0;
        int paddingRight = 0;

        private Builder() {}

        public Builder direction(Direction v) {
            this.direction = v;
            return this;
        }

        public Builder justify(Justify v) {
            this.justify = v;
            return this;
        }

        public Builder align(Align v) {
            this.align = v;
            return this;
        }

        public Builder wrap(boolean v) {
            this.wrap = v;
            return this;
        }

        public Builder wrap() {
            return wrap(true);
        }

        public Builder reverse(boolean v) {
            this.reverse = v;
            return this;
        }

        public Builder reverse() {
            return reverse(true);
        }

        public Builder gap(int v) {
            this.hgap = v;
            this.vgap = v;
            return this;
        }

        public Builder hgap(int v) {
            this.hgap = v;
            return this;
        }

        public Builder vgap(int v) {
            this.vgap = v;
            return this;
        }

        public Builder padding(int all) {
            this.paddingTop = this.paddingBottom = this.paddingLeft = this.paddingRight = all;
            return this;
        }

        public Builder padding(int vertical, int horizontal) {
            this.paddingTop = this.paddingBottom = vertical;
            this.paddingLeft = this.paddingRight = horizontal;
            return this;
        }

        public Builder padding(int top, int right, int bottom, int left) {
            this.paddingTop = top;
            this.paddingRight = right;
            this.paddingBottom = bottom;
            this.paddingLeft = left;
            return this;
        }

        public FlexBoxLayout build() {
            return new FlexBoxLayout(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FlexBoxLayout of(Consumer<Builder> config) {
        Builder b = new Builder();
        config.accept(b);
        return b.build();
    }

    public static class ScrollablePanel extends JPanel implements Scrollable {

        private final FlexBoxLayout layout;

        public ScrollablePanel(FlexBoxLayout layout) {
            super(layout);
            this.layout = layout;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public static ScrollablePanel scrollablePanel(Consumer<Builder> config) {
        FlexBoxLayout layout = of(config);
        return new ScrollablePanel(layout);
    }

    private final Direction direction;
    private final Justify justify;
    private final Align align;
    private final boolean wrap;
    private final boolean reverse;
    private final int hgap;
    private final int vgap;
    private final int paddingTop;
    private final int paddingBottom;
    private final int paddingLeft;
    private final int paddingRight;

    private final Map<Component, FlexConstraints> constraints = new IdentityHashMap<>();

    private FlexBoxLayout(Builder b) {
        this.direction = b.direction;
        this.justify = b.justify;
        this.align = b.align;
        this.wrap = b.wrap;
        this.reverse = b.reverse;
        this.hgap = b.hgap;
        this.vgap = b.vgap;
        this.paddingTop = b.paddingTop;
        this.paddingBottom = b.paddingBottom;
        this.paddingLeft = b.paddingLeft;
        this.paddingRight = b.paddingRight;
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraint) {
        constraints.put(comp, constraint instanceof FlexConstraints fc ? fc : new FlexConstraints());
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        constraints.put(comp, new FlexConstraints());
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        constraints.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Insets ins = parent.getInsets();
        int totalPadH = paddingLeft + paddingRight + ins.left + ins.right;
        int totalPadV = paddingTop + paddingBottom + ins.top + ins.bottom;

        List<Component> visible = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) visible.add(c);
        }
        if (visible.isEmpty()) return new Dimension(totalPadH, totalPadV);

        if (direction == Direction.COLUMN) {
            int w = 0, h = 0;
            for (Component c : visible) {
                Dimension d = c.getPreferredSize();
                w = Math.max(w, d.width);
                h += d.height + vgap;
            }
            h -= vgap;
            return new Dimension(w + totalPadH, h + totalPadV);
        }

        if (!wrap) {
            int w = 0, h = 0;
            for (Component c : visible) {
                Dimension d = c.getPreferredSize();
                w += d.width + hgap;
                h = Math.max(h, d.height);
            }
            w -= hgap;
            return new Dimension(w + totalPadH, h + totalPadV);
        }

        int availW = parent.getWidth() - totalPadH;
        if (availW <= 0) {
            int fallbackH = 0;
            for (Component c : visible) fallbackH = Math.max(fallbackH, c.getPreferredSize().height);
            return new Dimension(totalPadH, fallbackH + totalPadV);
        }

        List<List<Component>> lines = new ArrayList<>();
        List<Component> current = new ArrayList<>();
        int lineUsedW = 0;

        for (Component c : visible) {
            int cw = prefW(c, availW);
            int needed = current.isEmpty() ? cw : cw + hgap;
            if (!current.isEmpty() && lineUsedW + needed > availW) {
                lines.add(current);
                current = new ArrayList<>();
                lineUsedW = 0;
                needed = cw;
            }
            current.add(c);
            lineUsedW += needed;
        }
        if (!current.isEmpty()) lines.add(current);

        int totalH = 0;
        for (List<Component> line : lines) {
            int lineH = 0;
            for (Component c : line) lineH = Math.max(lineH, c.getPreferredSize().height);
            totalH += lineH + vgap;
        }
        totalH -= vgap;

        return new Dimension(availW + totalPadH, totalH + totalPadV);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container t) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container t) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container t) {}

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            if (direction == Direction.ROW) layoutRow(parent);
            else layoutColumn(parent);
        }
        parent.repaint();
    }

    private void layoutRow(Container parent) {
        Insets ins  = parent.getInsets();
        int availW  = parent.getWidth()  - ins.left - ins.right  - paddingLeft - paddingRight;
        int availH  = parent.getHeight() - ins.top  - ins.bottom - paddingTop  - paddingBottom;
        int originX = ins.left + paddingLeft;
        int originY = ins.top  + paddingTop;

        List<Component> all = visibleComponents(parent);
        if (reverse) Collections.reverse(all);

        List<List<Component>> lines = new ArrayList<>();
        List<Component> current = new ArrayList<>();
        int lineUsedW = 0;

        for (Component c : all) {
            int cw = prefW(c, availW);
            int needed = current.isEmpty() ? cw : cw + hgap;

            if (wrap && !current.isEmpty() && lineUsedW + needed > availW) {
                lines.add(current);
                current = new ArrayList<>();
                lineUsedW = 0;
                needed = cw;
            }
            current.add(c);
            lineUsedW += needed;
        }
        if (!current.isEmpty()) lines.add(current);

        int maxPerLine = lines.stream().mapToInt(List::size).max().orElse(1);

        int y = originY;
        for (int li = 0; li < lines.size(); li++) {
            List<Component> line = lines.get(li);
            boolean isIncomplete = li == lines.size() - 1 && line.size() < maxPerLine;

            int lineH = lineHeight(line, availH);
            int[] widths = resolveWidths(line, availW);
            int[] xs = justifyRow(line.size(), widths, availW, originX, isIncomplete);

            for (int i = 0; i < line.size(); i++) {
                Component c = line.get(i);
                int ch = compH(c, lineH);
                int cy = alignOffsetV(c, y, lineH, ch);
                c.setBounds(xs[i], cy, widths[i], ch);
            }
            y += lineH + vgap;
        }
    }

    private void layoutColumn(Container parent) {
        Insets ins  = parent.getInsets();
        int availW  = parent.getWidth()  - ins.left - ins.right  - paddingLeft - paddingRight;
        int availH  = parent.getHeight() - ins.top  - ins.bottom - paddingTop  - paddingBottom;
        int originX = ins.left + paddingLeft;
        int originY = ins.top  + paddingTop;

        List<Component> visible = visibleComponents(parent);
        if (reverse) Collections.reverse(visible);

        int[] heights = resolveHeights(visible, availH);
        int[] ys = justifyColumn(visible.size(), heights, availH, originY);

        for (int i = 0; i < visible.size(); i++) {
            Component c = visible.get(i);
            int cw = compW(c, availW);
            int cx = alignOffsetH(c, originX, availW, cw);
            c.setBounds(cx, ys[i], cw, heights[i]);
        }
    }

    private int[] justifyRow(int count, int[] sizes, int available, int origin, boolean isIncomplete) {
        return justifyAxis(count, sizes, available, origin, isIncomplete, hgap);
    }

    private int[] justifyColumn(int count, int[] sizes, int available, int origin) {
        return justifyAxis(count, sizes, available, origin, false, vgap);
    }

    private int[] justifyAxis(int count, int[] sizes, int available, int origin, boolean isIncomplete, int gap) {
        if (count == 0) return new int[0];

        int totalSizes = 0;
        for (int s : sizes) totalSizes += s;

        int[] pos = new int[count];
        Justify effective = justify;

        if (isIncomplete && isSpaceJustify(justify)) effective = Justify.CENTER;

        switch (effective) {
            case START -> {
                int p = origin;
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + gap;
                }
            }
            case END -> {
                int packed = totalSizes + gap * (count - 1);
                int p = origin + (available - packed);
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + gap;
                }
            }
            case CENTER -> {
                int packed = totalSizes + gap * (count - 1);
                int p = origin + (available - packed) / 2;
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + gap;
                }
            }
            case SPACE_BETWEEN -> {
                if (count == 1) {
                    pos[0] = origin + (available - sizes[0]) / 2;
                    break;
                }
                int between = (available - totalSizes) / (count - 1);
                int p = origin;
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + between;
                }
            }
            case SPACE_AROUND -> {
                int around = (available - totalSizes) / count;
                int p = origin + around / 2;
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + around;
                }
            }
            case SPACE_EVENLY -> {
                int evenly = (available - totalSizes) / (count + 1);
                int p = origin + evenly;
                for (int i = 0; i < count; i++) {
                    pos[i] = p;
                    p += sizes[i] + evenly;
                }
            }
        }
        return pos;
    }

    private boolean isSpaceJustify(Justify j) {
        return j == Justify.SPACE_BETWEEN || j == Justify.SPACE_AROUND || j == Justify.SPACE_EVENLY;
    }

    private int prefW(Component c, int availW) {
        FlexConstraints fc = fc(c);
        int w;
        if      (fc.fixedWidth   >= 0) w = fc.fixedWidth;
        else if (fc.widthPercent >= 0) w = (int)(availW * fc.widthPercent / 100.0);
        else if (fc.basis         > 0) w = fc.basis;
        else                           w = c.getPreferredSize().width;
        return applyWidthBounds(fc, clamp(w, 0, availW));
    }

    private int compH(Component c, int lineH) {
        FlexConstraints fc = fc(c);
        Align effective = fc.alignSelf != null ? fc.alignSelf : align;
        int h;
        if      (fc.fixedHeight   >= 0)             h = fc.fixedHeight;
        else if (fc.heightPercent >= 0)             h = (int)(lineH * fc.heightPercent / 100.0);
        else if (effective        == Align.STRETCH) h = lineH;
        else                                        h = c.getPreferredSize().height;
        return applyHeightBounds(fc, clamp(h, 0, lineH));
    }

    private int compW(Component c, int availW) {
        FlexConstraints fc = fc(c);
        Align effective = fc.alignSelf != null ? fc.alignSelf : align;
        int w;
        if      (fc.fixedWidth   >= 0)             w = fc.fixedWidth;
        else if (fc.widthPercent >= 0)             w = (int)(availW * fc.widthPercent / 100.0);
        else if (effective       == Align.STRETCH) w = availW;
        else if (fc.basis         > 0)             w = fc.basis;
        else                                       w = c.getPreferredSize().width;
        return applyWidthBounds(fc, clamp(w, 0, availW));
    }

    private int prefH(Component c, int availH) {
        FlexConstraints fc = fc(c);
        int h;
        if      (fc.fixedHeight   >= 0) h = fc.fixedHeight;
        else if (fc.heightPercent >= 0) h = (int)(availH * fc.heightPercent / 100.0);
        else if (fc.basis          > 0) h = fc.basis;
        else                            h = c.getPreferredSize().height;
        return applyHeightBounds(fc, clamp(h, 0, availH));
    }

    private int applyWidthBounds(FlexConstraints fc, int w) {
        if (fc.minWidth >= 0) w = Math.max(w, fc.minWidth);
        if (fc.maxWidth >= 0) w = Math.min(w, fc.maxWidth);
        return w;
    }

    private int applyHeightBounds(FlexConstraints fc, int h) {
        if (fc.minHeight >= 0) h = Math.max(h, fc.minHeight);
        if (fc.maxHeight >= 0) h = Math.min(h, fc.maxHeight);
        return h;
    }

    private int lineHeight(List<Component> line, int availH) {
        int h = 0;
        for (Component c : line) h = Math.max(h, prefH(c, availH));
        return h;
    }

    private int[] resolveWidths(List<Component> line, int availW) {
        int n = line.size();
        int[] widths = new int[n];
        int usedW = 0;
        int totalGrow = 0;

        for (int i = 0; i < n; i++) {
            widths[i]  = prefW(line.get(i), availW);
            usedW     += widths[i];
            totalGrow += fc(line.get(i)).grow;
        }
        usedW += hgap * (n - 1);

        int free = availW - usedW;
        if (totalGrow > 0 && free > 0) {
            for (int i = 0; i < n; i++) {
                int g = fc(line.get(i)).grow;
                if (g > 0) widths[i] += (int)(free * (double) g / totalGrow);
            }
        }
        return widths;
    }

    private int[] resolveHeights(List<Component> comps, int availH) {
        int[] heights = new int[comps.size()];
        int usedH = 0;
        int totalGrow = 0;

        for (int i = 0; i < comps.size(); i++) {
            heights[i]  = prefH(comps.get(i), availH);
            usedH      += heights[i];
            totalGrow  += fc(comps.get(i)).grow;
        }
        usedH += vgap * (comps.size() - 1);

        int free = availH - usedH;
        if (totalGrow > 0 && free > 0) {
            for (int i = 0; i < comps.size(); i++) {
                int g = fc(comps.get(i)).grow;
                if (g > 0) heights[i] += (int)(free * (double) g / totalGrow);
            }
        }
        return heights;
    }

    private int alignOffsetV(Component c, int lineY, int lineH, int compH) {
        Align effective = fc(c).alignSelf != null ? fc(c).alignSelf : align;
        return switch (effective) {
            case END    -> lineY + lineH - compH;
            case CENTER -> lineY + (lineH - compH) / 2;
            default     -> lineY;
        };
    }

    private int alignOffsetH(Component c, int startX, int availW, int compW) {
        Align effective = fc(c).alignSelf != null ? fc(c).alignSelf : align;
        return switch (effective) {
            case END    -> startX + availW - compW;
            case CENTER -> startX + (availW - compW) / 2;
            default     -> startX;
        };
    }

    private FlexConstraints fc(Component c) {
        return constraints.getOrDefault(c, new FlexConstraints());
    }

    private List<Component> visibleComponents(Container parent) {
        List<Component> list = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) list.add(c);
        }
        return list;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}