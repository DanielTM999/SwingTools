package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineMarkerLayer implements GutterLayer {

    private final Map<Integer, Color> markers = new HashMap<>();

    public interface ClickListener {
        void onClick(MouseEvent event, int line, Color color);
    }

    public enum Side {
        LEFT,
        RIGHT
    }

    @Getter
    @Setter
    @Builder.Default
    private int stripeWidth = 3;

    @Getter
    @Setter
    @Builder.Default
    private int stripeOffsetX = 0;

    @Getter
    @Setter
    @Builder.Default
    private Side side = Side.LEFT;

    @Getter
    @Setter
    @Builder.Default
    private int clickTolerance = 4;

    @Getter
    @Setter
    @Builder.Default
    private ClickListener onClick = null;

    public void setLineColor(int line, Color color) {
        if (color == null) {
            markers.remove(line);
        } else {
            markers.put(line, color);
        }
    }

    public void removeLineColor(int line) {
        markers.remove(line);
    }

    public void clearLineColors() {
        markers.clear();
    }

    public Color getLineColor(int line) {
        return markers.get(line);
    }

    public boolean hasLineColor(int line) {
        return markers.containsKey(line);
    }

    public boolean isLineMarkerClick(CodeEditorGutter gutter, int mouseX) {
        int gutterWidth = gutter.getWidth() > 0 ? gutter.getWidth() : gutter.getGutterWidth();
        int stripeX = getStripeX(gutter, 0, gutterWidth);
        return mouseX >= stripeX - clickTolerance
                && mouseX <= stripeX + stripeWidth + clickTolerance;
    }

    @Override
    public void onLinesInserted(int atLine, int count) {
        Map<Integer, Color> updated = new HashMap<>();
        for (var entry : markers.entrySet()) {
            int line = entry.getKey();
            if (line >= atLine) {
                updated.put(line + count, entry.getValue());
            } else {
                updated.put(line, entry.getValue());
            }
        }
        markers.clear();
        markers.putAll(updated);
    }

    @Override
    public void onLinesRemoved(int atLine, int count) {
        Map<Integer, Color> updated = new HashMap<>();
        for (var entry : markers.entrySet()) {
            int line = entry.getKey();
            if (line >= atLine && line < atLine + count) {
                continue;
            } else if (line >= atLine + count) {
                updated.put(line - count, entry.getValue());
            } else {
                updated.put(line, entry.getValue());
            }
        }
        markers.clear();
        markers.putAll(updated);
    }

    @Override
    public void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height) {
        Color color = markers.get(line);
        if (color == null) return;
        g.setColor(color);
        g.fillRect(getStripeX(gutter, x, width), y, stripeWidth, height);
    }

    @Override
    public void onMouseClick(MouseEvent e, int line) {
        if (onClick == null) return;
        Color color = markers.get(line);
        if (color == null) return;
        onClick.onClick(e, line, color);
    }

    private int getStripeX(CodeEditorGutter gutter, int x, int width) {
        if (side == Side.RIGHT) {
            return x + width - stripeWidth - stripeOffsetX;
        }
        return x + stripeOffsetX;
    }
}
