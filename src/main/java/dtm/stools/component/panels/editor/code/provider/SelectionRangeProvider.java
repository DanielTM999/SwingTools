package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Range;

import java.util.List;


@FunctionalInterface
public interface SelectionRangeProvider extends CodeEditorProvider {

    List<Range> getSelectionRanges(String buffer, int offset);
}
