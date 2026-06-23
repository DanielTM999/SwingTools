package dtm.stools.component.panels.editor.code.diagnostics;

import java.awt.Color;

public enum DiagnosticSeverity {
    ERROR(new Color(220, 70, 70)),
    WARNING(new Color(230, 170, 40)),
    INFO(new Color(70, 140, 220)),
    HINT(new Color(120, 180, 120));

    public final Color defaultColor;

    DiagnosticSeverity(Color color) {
        this.defaultColor = color;
    }
}
