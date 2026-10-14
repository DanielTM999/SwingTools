package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnostic;

import javax.swing.text.Position;

record TextAreaDiagnosticEntry(TextDiagnostic diagnostic, Position start, Position end, Object tag, boolean manual) {

    TextDiagnostic live() {
        int from = start.getOffset();
        int to = end.getOffset();
        if (to <= from) return null;
        return diagnostic.withRange(from, to);
    }

    TextAreaDiagnosticEntry withTag(Object nextTag) {
        return new TextAreaDiagnosticEntry(diagnostic, start, end, nextTag, manual);
    }
}
