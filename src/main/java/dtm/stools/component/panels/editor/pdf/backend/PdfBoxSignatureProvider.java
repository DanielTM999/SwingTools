package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfSignatureStatus;
import dtm.stools.component.panels.editor.pdf.api.PdfSignatureValidation;
import dtm.stools.component.panels.editor.pdf.provider.PdfSignatureProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfTrustProvider;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.*;
import java.util.*;

public final class PdfBoxSignatureProvider implements PdfSignatureProvider {
    @Override
    public String id() { return "pdf.signature.pdfbox"; }
    @Override
    public void sign(Path source, Path destination, Path pkcs12, char[] password, String reason) throws IOException {
        Objects.requireNonNull(source); Objects.requireNonNull(destination); Objects.requireNonNull(pkcs12);
        try {
            KeyStore store = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(pkcs12)) { store.load(input, password); }
            String alias = Collections.list(store.aliases()).stream().filter(a -> {
                try { return store.isKeyEntry(a); } catch (Exception error) { return false; }
            }).findFirst().orElseThrow(() -> new IOException("PKCS#12 sem chave privada"));
            PrivateKey key = (PrivateKey) store.getKey(alias, password);
            java.security.cert.Certificate[] chain = store.getCertificateChain(alias);
            if (key == null || chain == null || chain.length == 0) throw new IOException("Certificado de assinatura incompleto");
            Path target = destination.toAbsolutePath();
            Path temporary = Files.createTempFile(target.getParent(), ".swingtools-signature-", ".tmp");
            try {
                try (PDDocument document = Loader.loadPDF(source.toFile()); var output = Files.newOutputStream(temporary)) {
                    PDSignature signature = new PDSignature();
                    signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                    signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                    signature.setName(((X509Certificate)chain[0]).getSubjectX500Principal().getName());
                    signature.setReason(reason == null ? "" : reason);
                    signature.setSignDate(Calendar.getInstance());
                    document.addSignature(signature, content -> {
                        try {
                            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
                            String algorithm = key.getAlgorithm().equalsIgnoreCase("EC") ? "SHA256withECDSA" : "SHA256withRSA";
                            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().build()).build(
                                    new JcaContentSignerBuilder(algorithm).build(key), (X509Certificate)chain[0]));
                            generator.addCertificates(new JcaCertStore(Arrays.asList(chain)));
                            return generator.generate(new CMSProcessableByteArray(content.readAllBytes()), false).getEncoded();
                        } catch (Exception error) { throw new IOException("Falha ao assinar PDF", error); }
                    });
                    document.saveIncremental(output);
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException error) { throw error; }
        catch (Exception error) { throw new IOException("Falha ao carregar PKCS#12", error); }
    }
    @Override
    public List<PdfSignatureValidation> validate(Path source, PdfTrustProvider trust) throws IOException {
        byte[] bytes = Files.readAllBytes(source);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            List<PdfSignatureValidation> results = new ArrayList<>();
            for (PDSignature signature : document.getSignatureDictionaries()) {
                String name = signature.getName() == null ? "Assinante desconhecido" : signature.getName();
                try {
                    byte[] contents = signature.getContents(bytes);
                    byte[] signed = signature.getSignedContent(bytes);
                    CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signed),
                            new ByteArrayInputStream(contents));
                    SignerInformation signer = cms.getSignerInfos().getSigners().iterator().next();
                    var matches = cms.getCertificates().getMatches(signer.getSID());
                    if (matches.isEmpty()) { results.add(new PdfSignatureValidation(name, PdfSignatureStatus.INVALID, "Certificado ausente", null)); continue; }
                    X509Certificate certificate = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                            .getCertificate((org.bouncycastle.cert.X509CertificateHolder)matches.iterator().next());
                    boolean intact = signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificate));
                    if (!intact) { results.add(new PdfSignatureValidation(name, PdfSignatureStatus.INVALID, "Conteúdo assinado alterado", certificate)); continue; }
                    try { certificate.checkValidity(); }
                    catch (CertificateExpiredException | CertificateNotYetValidException error) {
                        results.add(new PdfSignatureValidation(name, PdfSignatureStatus.INVALID, "Certificado fora da validade", certificate)); continue;
                    }
                    int[] range = signature.getByteRange();
                    if (range == null || range.length != 4 || range[0] != 0 || range[1] < 0
                            || range[2] < range[1] || range[3] < 0
                            || (long) range[2] + range[3] != bytes.length) {
                        results.add(new PdfSignatureValidation(name, PdfSignatureStatus.INVALID,
                                "Intervalos assinados inválidos ou alterações posteriores à assinatura", certificate)); continue;
                    }
                    try {
                        X509CertSelector selector = new X509CertSelector(); selector.setCertificate(certificate);
                        PKIXBuilderParameters parameters = new PKIXBuilderParameters(trust.anchors(), selector);
                        List<X509Certificate> certificates = new ArrayList<>();
                        for (var holder : cms.getCertificates().getMatches(null)) certificates.add(
                                new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(holder));
                        parameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certificates)));
                        parameters.setRevocationEnabled(trust.checkRevocation());
                        CertPathBuilder.getInstance("PKIX").build(parameters);
                        results.add(new PdfSignatureValidation(name, PdfSignatureStatus.VALID, "Assinatura e cadeia verificadas", certificate));
                    } catch (CertPathBuilderException error) {
                        Throwable cause = error;
                        while (cause.getCause() != null) cause = cause.getCause();
                        PdfSignatureStatus status = cause instanceof CertPathValidatorException invalid
                                && invalid.getReason() == CertPathValidatorException.BasicReason.REVOKED
                                ? PdfSignatureStatus.INVALID : PdfSignatureStatus.INCONCLUSIVE;
                        results.add(new PdfSignatureValidation(name, status, "Cadeia ou revogação não verificável: " + error.getMessage(), certificate));
                    }
                } catch (Exception error) { results.add(new PdfSignatureValidation(name, PdfSignatureStatus.INVALID, error.getMessage(), null)); }
            }
            return List.copyOf(results);
        }
    }
}
