package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;

import java.util.List;

public final class PdfElementFactories {
    private PdfElementFactories() {}

    public static List<PdfElementFactory> defaults() {
        return List.of(
                new TextElementFactory(),
                new ImageElementFactory(),
                new ShapeElementFactory(PdfShapeKind.RECTANGLE),
                new ShapeElementFactory(PdfShapeKind.ELLIPSE),
                new ShapeElementFactory(PdfShapeKind.LINE),
                new ShapeElementFactory(PdfShapeKind.ARROW),
                new InkElementFactory(),
                new NoteElementFactory(),
                new HighlightElementFactory(),
                new FormFieldElementFactory(PdfFormFieldKind.TEXT),
                new FormFieldElementFactory(PdfFormFieldKind.CHECKBOX),
                new FormFieldElementFactory(PdfFormFieldKind.CHOICE),
                new FormFieldElementFactory(PdfFormFieldKind.RADIO),
                new VisualSignatureElementFactory());
    }
}
