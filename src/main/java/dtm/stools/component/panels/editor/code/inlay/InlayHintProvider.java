package dtm.stools.component.panels.editor.code.inlay;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;

@FunctionalInterface
public interface InlayHintProvider extends CodeEditorProvider {

    List<InlayHint> getInlayHints(InlayHintContext context);
}
