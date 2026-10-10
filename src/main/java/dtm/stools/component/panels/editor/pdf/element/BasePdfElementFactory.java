package dtm.stools.component.panels.editor.pdf.element;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.awt.Cursor;

@Getter
@Accessors(fluent = true)
public abstract class BasePdfElementFactory implements PdfElementFactory {
    private final String id;
    private final String commandId;
    private final String title;
    private final String icon;
    private final String tip;
    private final PdfPlacementMode placementMode;

    protected BasePdfElementFactory(String id, String commandId, String title, String icon, String tip,
                                    PdfPlacementMode placementMode) {
        this.id = id;
        this.commandId = commandId;
        this.title = title;
        this.icon = icon;
        this.tip = tip;
        this.placementMode = placementMode;
    }

    @Override
    public boolean ribbonItem() { return false; }
    @Override
    public int cursor() { return Cursor.CROSSHAIR_CURSOR; }
}
