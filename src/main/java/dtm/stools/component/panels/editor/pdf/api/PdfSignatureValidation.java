package dtm.stools.component.panels.editor.pdf.api;

import java.security.cert.X509Certificate;

public record PdfSignatureValidation(String signer, PdfSignatureStatus status, String reason, X509Certificate certificate) {}
