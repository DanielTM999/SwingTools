package dtm.stools.component.panels.editor.sheet.model;

public interface SheetObject {
    String id();
    ObjectAnchor anchor();
    SheetObject withAnchor(ObjectAnchor anchor);
    String description();
}
