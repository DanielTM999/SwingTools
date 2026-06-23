package dtm.stools.component.panels.editor.code.hover;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

@FunctionalInterface
public interface HoverDocumentationProvider extends CodeEditorProvider {

    HoverInfo provideHover(HoverDocumentationContext context);
}
