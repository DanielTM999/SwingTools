package dtm.stools.component.menu.popup.style;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionMenuStyle {

    private Color background;
    private Color foreground;
    private Color selectionBackground;
    private Color selectionForeground;

    public boolean isEmpty() {
        return background == null
                && foreground == null
                && selectionBackground == null
                && selectionForeground == null;
    }

    public ActionMenuStyle background(Color background) {
        this.background = background;
        return this;
    }

    public ActionMenuStyle foreground(Color foreground) {
        this.foreground = foreground;
        return this;
    }

    public ActionMenuStyle selectionBackground(Color selectionBackground) {
        this.selectionBackground = selectionBackground;
        return this;
    }

    public ActionMenuStyle selectionForeground(Color selectionForeground) {
        this.selectionForeground = selectionForeground;
        return this;
    }

    public ActionMenuStyle colors(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
        return this;
    }

    public ActionMenuStyle selectionColors(Color background, Color foreground) {
        this.selectionBackground = background;
        this.selectionForeground = foreground;
        return this;
    }

}