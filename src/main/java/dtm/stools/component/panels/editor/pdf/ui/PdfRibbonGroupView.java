package dtm.stools.component.panels.editor.pdf.ui;

import javax.swing.JButton;
import javax.swing.JComponent;

final class PdfRibbonGroupView {
    final PdfRibbonGroup group;
    final JComponent full;
    final JComponent compact;
    final JButton collapsed;
    PdfRibbonMode mode = PdfRibbonMode.FULL;

    PdfRibbonGroupView(PdfRibbonGroup group, JComponent full, JComponent compact, JButton collapsed) {
        this.group = group;
        this.full = full;
        this.compact = compact;
        this.collapsed = collapsed;
    }

    JComponent current() {
        return switch (mode) {
            case COMPACT -> group.items().stream().allMatch(item -> item.custom() != null) ? collapsed : compact;
            case COLLAPSED, OVERFLOW -> collapsed;
            default -> full;
        };
    }
}
