package dtm.stools.component.panels.editor.code.codelens;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.Color;
import java.awt.Cursor;
import java.util.function.Consumer;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CodeLensItem {

    private String text;
    private Color foreground;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private Cursor cursor;
    private Consumer<CodeLensClickEvent> onClick;
    private String tooltip;
}
