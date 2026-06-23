package dtm.stools.component.panels.editor.code.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.Color;
import java.awt.Cursor;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WordHoverStyle {

    public static final WordHoverStyle DEFAULT = new WordHoverStyle();

    private Color foreground;
    private Color background;

    private boolean underline;
    private Color underlineColor;
    private int underlineThickness;

    private boolean box;
    private Color boxColor;
    private int boxThickness;

    private boolean bold;
    private boolean italic;

    private Cursor cursor;

    public static WordHoverStyle defaultStyle() {
        return WordHoverStyle.builder()
                .underline(true)
                .underlineThickness(1)
                .boxThickness(1)
                .cursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                .build();
    }
}
