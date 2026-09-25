package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.provider.SheetConfirmationProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetConfirmationRequest;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;

public final class DefaultConfirmationProvider implements SheetConfirmationProvider {
    @Override public String id() { return "sheet.popup.confirmation.default"; }

    @Override
    public int confirm(SheetConfirmationRequest request) {
        SheetDialogActivity<Integer> d = new SheetDialogActivity<>(request.owner(), request.title(), Dialog.ModalityType.DOCUMENT_MODAL);
        int[] choice = {-1};
        JPanel content = new JPanel(new BorderLayout(10, 0));
        JLabel icon = new JLabel(request.warning() ? "⚠" : "ℹ");
        icon.setFont(icon.getFont().deriveFont(26f));
        icon.setForeground(request.warning() ? UiTokens.warning() : UiTokens.info());
        content.add(icon, BorderLayout.WEST);
        JLabel text = new JLabel("<html><div style='width:360px'>" + request.message().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</div></html>");
        content.add(text, BorderLayout.CENTER);
        content.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        d.setBody(content);
        for (int k = 0; k < request.options().size(); k++) {
            int idx = k;
            var b = d.addAction(request.options().get(k), () -> { choice[0] = idx; d.dispose(); }, k == request.defaultOption());
            if (k == request.defaultOption()) d.getRootPane().setDefaultButton(b);
        }
        try { d.open(); } finally { d.dispose(); }
        return choice[0];
    }
}
