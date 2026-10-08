package dtm.stools.component.panels.editor.pdf.provider;

import java.security.cert.TrustAnchor;
import java.util.Set;

public interface PdfTrustProvider extends PdfProvider {
    Set<TrustAnchor> anchors();
    default boolean checkRevocation() { return true; }
}
