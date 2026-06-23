package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.editor.code.listeners.BreakpointChangeListener;
import dtm.stools.component.panels.editor.code.prototype.Breakpoint;
import dtm.stools.component.panels.editor.code.prototype.styles.BreakpointStyle;
import lombok.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BreakpointLayer implements GutterLayer, TransferableLayer {

    private static final int MIN_WIDTH_FOR_SIDE = 40;

    @Builder.Default
    private final Map<Integer, Breakpoint> breakpoints = new HashMap<>();

    @Builder.Default
    private final List<BreakpointChangeListener> breakpointListeners = new ArrayList<>();

    @Setter
    @Builder.Default
    private BreakpointStyle defaultStyle = BreakpointStyle.builder().build();


    @Setter
    @Builder.Default
    private BreakpointStyle defaultInactiveStyle = null;

    @Getter
    @Setter
    @Builder.Default
    private boolean enableOnClick = true;

    @Getter
    @Setter
    @Builder.Default
    private boolean overlay = false;

    @Getter
    @Setter
    @Builder.Default
    private int overlayOffsetX = 8;

    @Getter
    @Setter
    @Builder.Default
    private int sideOffsetX = 0;

    @Getter
    @Setter
    @Builder.Default
    private boolean previewOnHoverEnabled = false;

    @Getter
    @Setter
    @Builder.Default
    private float previewAlpha = 0.35f;

    @Getter
    @Setter
    private int hoverLine = -1;

    public void clearHover() {
        this.hoverLine = -1;
    }

    public boolean isEffectiveOverlay(int gutterWidth) {
        return overlay || gutterWidth < MIN_WIDTH_FOR_SIDE;
    }

    public void setInactiveIcon(Icon icon) {
        this.defaultInactiveStyle = BreakpointStyle.builder().icon(icon).build();
    }

    public void addBreakpoint(int line) {
        addBreakpoint(line, defaultStyle, null);
    }

    public void addBreakpoint(int line, BreakpointStyle style) {
        addBreakpoint(line, style, null);
    }

    public void addBreakpoint(int line, BreakpointStyle style, BreakpointStyle inactiveStyle) {
        setBreakpoint(buildBreakpoint(line, true, style, inactiveStyle));
    }

    public void addBreakpoint(int line, Color color) {
        addBreakpoint(line, BreakpointStyle.builder().color(color).build(), null);
    }

    public void addBreakpoint(int line, Color color, Color inactiveColor) {
        addBreakpoint(line,
                BreakpointStyle.builder().color(color).build(),
                BreakpointStyle.builder().color(inactiveColor).build());
    }

    public void addBreakpoint(int line, Icon icon) {
        addBreakpoint(line, BreakpointStyle.builder().icon(icon).build(), null);
    }

    public void addBreakpoint(int line, Icon icon, Icon inactiveIcon) {
        addBreakpoint(line,
                BreakpointStyle.builder().icon(icon).build(),
                BreakpointStyle.builder().icon(inactiveIcon).build());
    }

    public void removeBreakpoint(int line) {
        Breakpoint removed = breakpoints.remove(line);
        if (removed != null) {
            fireBreakpointChanged(removed, false);
        }
    }

    public void clearBreakpoints() {
        if (breakpoints.isEmpty()) return;
        List<Breakpoint> removed = List.copyOf(breakpoints.values());
        breakpoints.clear();
        removed.forEach(bp -> fireBreakpointChanged(bp, false));
    }

    public void toggleBreakpoint(int line) {
        if (breakpoints.containsKey(line)) removeBreakpoint(line);
        else addBreakpoint(line);
    }

    public void setBreakpointActive(int line, boolean active) {
        Breakpoint current = breakpoints.get(line);
        if (current == null || current.isActive() == active) return;
        Breakpoint updated = current.toBuilder().active(active).build();
        breakpoints.put(line, updated);
        fireBreakpointChanged(updated, true);
    }

    public void toggleBreakpointActive(int line) {
        Breakpoint current = breakpoints.get(line);
        if (current != null) {
            setBreakpointActive(line, !current.isActive());
        }
    }

    public void deactivateBreakpoint(int line) {
        setBreakpointActive(line, false);
    }

    public void deactivateAllBreakpoints() {
        for (int line : new ArrayList<>(breakpoints.keySet())) {
            setBreakpointActive(line, false);
        }
    }

    public void activateBreakpoint(int line) {
        setBreakpointActive(line, true);
    }

    public void activateAllBreakpoints() {
        for (int line : new ArrayList<>(breakpoints.keySet())) {
            setBreakpointActive(line, true);
        }
    }

    public boolean hasBreakpoint(int line) {
        return breakpoints.containsKey(line);
    }

    public boolean isBreakpointActive(int line) {
        Breakpoint bp = breakpoints.get(line);
        return bp != null && bp.isActive();
    }

    public Set<Breakpoint> getBreakpoints() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(breakpoints.values()));
    }

    public Set<Integer> getBreakpointLines() {
        return Collections.unmodifiableSet(breakpoints.keySet());
    }

    public void addBreakpointChangeListener(BreakpointChangeListener listener) {
        if (listener != null && !breakpointListeners.contains(listener)) {
            breakpointListeners.add(listener);
        }
    }

    public void removeBreakpointChangeListener(BreakpointChangeListener listener) {
        breakpointListeners.remove(listener);
    }

    @Override
    public List<Object> getTransferableListeners() {
        return new ArrayList<>(breakpointListeners);
    }

    @Override
    public void receiveTransferableListeners(List<Object> listeners) {
        if (listeners == null) return;
        for (Object listener : listeners) {
            if (listener instanceof BreakpointChangeListener bpListener
                    && !breakpointListeners.contains(bpListener)) {
                breakpointListeners.add(bpListener);
            }
        }
    }

    protected void fireBreakpointChanged(Breakpoint breakpoint, boolean added) {
        for (BreakpointChangeListener listener : List.copyOf(breakpointListeners)) {
            listener.onBreakpointChanged(breakpoint, added);
        }
    }

    private Breakpoint buildBreakpoint(int line, boolean active, BreakpointStyle style, BreakpointStyle inactiveStyle) {
        return Breakpoint.builder()
                .line(line)
                .active(active)
                .style(style != null ? style : defaultStyle)
                .inactiveStyle(inactiveStyle != null ? inactiveStyle : defaultInactiveStyle)
                .build();
    }

    private void setBreakpoint(Breakpoint breakpoint) {
        boolean added = !breakpoints.containsKey(breakpoint.getLine());
        breakpoints.put(breakpoint.getLine(), breakpoint);
        if (added) {
            fireBreakpointChanged(breakpoint, true);
        }
    }

    @Override
    public void onLinesInserted(int atLine, int count) {
        Map<Integer, Breakpoint> updated = new HashMap<>();
        List<Breakpoint[]> moved = new ArrayList<>();
        for (var entry : breakpoints.entrySet()) {
            int line = entry.getKey();
            Breakpoint bp = entry.getValue();
            if (line >= atLine) {
                Breakpoint movedBp = bp.toBuilder().line(line + count).build();
                updated.put(movedBp.getLine(), movedBp);
                moved.add(new Breakpoint[]{bp, movedBp});
            } else {
                updated.put(line, bp);
            }
        }
        breakpoints.clear();
        breakpoints.putAll(updated);
        for (Breakpoint[] move : moved) {
            fireBreakpointChanged(move[0], false);
            fireBreakpointChanged(move[1], true);
        }
    }

    @Override
    public void onLinesRemoved(int atLine, int count) {
        Map<Integer, Breakpoint> updated = new HashMap<>();
        List<Breakpoint> removed = new ArrayList<>();
        List<Breakpoint[]> moved = new ArrayList<>();
        for (var entry : breakpoints.entrySet()) {
            int line = entry.getKey();
            Breakpoint bp = entry.getValue();
            if (line >= atLine && line < atLine + count) {
                removed.add(bp);
            } else if (line >= atLine + count) {
                Breakpoint movedBp = bp.toBuilder().line(line - count).build();
                updated.put(movedBp.getLine(), movedBp);
                moved.add(new Breakpoint[]{bp, movedBp});
            } else {
                updated.put(line, bp);
            }
        }
        breakpoints.clear();
        breakpoints.putAll(updated);
        removed.forEach(bp -> fireBreakpointChanged(bp, false));
        for (Breakpoint[] move : moved) {
            fireBreakpointChanged(move[0], false);
            fireBreakpointChanged(move[1], true);
        }
    }

    @Override
    public void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height) {
        Breakpoint breakpoint = breakpoints.get(line);
        boolean isPreview = breakpoint == null && previewOnHoverEnabled && hoverLine == line;
        if (breakpoint == null && !isPreview) return;

        boolean active = breakpoint == null || breakpoint.isActive();
        BreakpointStyle effectiveStyle = breakpoint != null ? breakpoint.getEffectiveStyle() : defaultStyle;
        boolean effective = isEffectiveOverlay(gutter.getGutterWidth());
        int size = height - 4;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (isPreview) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, previewAlpha));
            }

            if (effectiveStyle.getIcon() != null) {
                Icon icon = effectiveStyle.getIcon();
                int ix = effective
                        ? getXForLineOverlay(g2, gutter, x, icon.getIconWidth(), width)
                        : x + sideOffsetX;
                int iy = y + (height - icon.getIconHeight()) / 2;
                icon.paintIcon(null, g2, ix, iy);
                return;
            }

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = effective ? getXForLineOverlay(g2, gutter, x, size, width) : x + sideOffsetX;
            int cy = y + (height - size) / 2;
            g2.setColor(effectiveStyle.getColor());
            if (active) {
                g2.fillOval(cx, cy, size, size);
            } else {
                float stroke = Math.max(1f, size * 0.12f);
                int inset = Math.max(1, Math.round(stroke / 2f));
                g2.setStroke(new BasicStroke(stroke));
                g2.drawOval(cx + inset, cy + inset, size - 2 * inset, size - 2 * inset);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void onMouseClick(MouseEvent e, int line) {
        if (!enableOnClick) return;
        toggleBreakpoint(line);
    }

    protected int getXForLineOverlay(Graphics g, CodeEditorGutter gutter, int baseX, int size, int width) {
        int areaWidth = gutter.getLineNumberAreaWidth();
        if (areaWidth <= 0) {
            return baseX + overlayOffsetX;
        }
        return baseX + Math.max(0, (areaWidth - size) / 2);
    }


}
