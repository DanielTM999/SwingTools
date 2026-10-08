package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.provider.PdfTrustProvider;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.util.HashSet;
import java.util.Set;

public final class JvmPdfTrustProvider implements PdfTrustProvider {
    @Override public String id() { return "pdf.trust.jvm"; }
    @Override public Set<TrustAnchor> anchors() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            Set<TrustAnchor> anchors = new HashSet<>();
            for (TrustManager manager : factory.getTrustManagers())
                if (manager instanceof X509TrustManager x509)
                    for (var certificate : x509.getAcceptedIssuers()) anchors.add(new TrustAnchor(certificate, null));
            return Set.copyOf(anchors);
        } catch (Exception error) { throw new IllegalStateException("Não foi possível carregar o truststore da JVM", error); }
    }
}
