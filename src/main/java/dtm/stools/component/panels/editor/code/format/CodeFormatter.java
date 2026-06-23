package dtm.stools.component.panels.editor.code.format;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

@FunctionalInterface
public interface CodeFormatter extends CodeEditorProvider {

    String format(FormatContext context);
}


