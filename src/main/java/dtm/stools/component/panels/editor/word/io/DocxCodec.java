package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import java.io.*;
import java.util.Objects;

public class DocxCodec {
    public static final String W=DocxNames.W;
    public static final String STRICT=DocxNames.W_STRICT;
    private final OpcPackage.Limits limits;
    public DocxCodec() { this(OpcPackage.Limits.DEFAULT); }
    public DocxCodec(OpcPackage.Limits limits) { this.limits=Objects.requireNonNull(limits); }
    public WordImportResult read(InputStream input) throws IOException {
        try { return new DocxReader(limits).read(input); }
        catch(IllegalArgumentException e) { throw new IOException("Invalid DOCX property",e); }
    }
    public void write(WordDocument document,OutputStream output) throws IOException { write(document,null,output,null); }
    public void write(WordDocument document,WordImportResult origin,OutputStream output) throws IOException { write(document,origin,output,null); }
    public void write(WordDocument document,WordImportResult origin,OutputStream output,WordObjectRegistry registry) throws IOException {
        Objects.requireNonNull(document); Objects.requireNonNull(output);
        if(origin!=null && document.equals(origin.document())) { output.write(origin.originalBytes()); return; }
        if(origin!=null && !origin.isEditable()) throw new IOException("The document is protected: "+String.join("; ",origin.blockingReasons()));
        try { new DocxWriter(document,origin,registry).write(output); }
        catch(IllegalArgumentException e) { throw new IOException("Cannot serialize DOCX",e); }
    }
}
