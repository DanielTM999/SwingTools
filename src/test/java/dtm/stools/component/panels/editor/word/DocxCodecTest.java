package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.io.*;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DocxCodecTest {
    private final DocxCodec codec=new DocxCodec();
    private byte[] write(WordDocument doc)throws IOException{var out=new ByteArrayOutputStream();codec.write(doc,out);return out.toByteArray();}
    private byte[] patch(byte[] bytes,String original,String replacement)throws IOException{
        OpcPackage source=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);
        String xml=new String(source.part("word/document.xml"),StandardCharsets.UTF_8);
        var out=new ByteArrayOutputStream();source.withPart("word/document.xml",xml.replace(original,replacement).getBytes(StandardCharsets.UTF_8)).write(out);return out.toByteArray();
    }
    @Test void nativeRoundTripPreservesTextRunsFormattingAndPageGeometry()throws Exception{
        WordDocument doc=WordDocument.fromText("Olá & <mundo> 👋\nSegundo parágrafo\n");
        doc=doc.format(0,3,s->s.withBold(true).withColor(0x1122ee));doc=doc.formatParagraphs(0,3,s->s.withAlignment(WordParagraphStyle.Alignment.CENTER).withHeadingLevel(1));
        WordImportResult read=codec.read(new ByteArrayInputStream(write(doc)));
        assertTrue(read.isEditable(),read.diagnostics().toString());assertEquals(doc.text(),read.document().text());
        assertEquals(doc.paragraphs().getFirst().runs(),read.document().paragraphs().getFirst().runs());
        assertEquals(doc.paragraphs().getFirst().style(),read.document().paragraphs().getFirst().style());
        assertEquals(doc.pageSettings().width(),read.document().pageSettings().width(),.05);
    }
    @Test void uneditedSaveIsByteExactEvenWithUnsupportedObjects()throws Exception{
        byte[] bytes=patch(write(WordDocument.fromText("safe")),"</w:body>","<w:tbl><w:tr><w:tc><w:p><w:r><w:t>cell</w:t></w:r></w:p></w:tc></w:tr></w:tbl></w:body>");
        WordImportResult read=codec.read(new ByteArrayInputStream(bytes));assertFalse(read.isEditable());
        ByteArrayOutputStream out=new ByteArrayOutputStream();codec.write(read.document(),read,out);assertArrayEquals(bytes,out.toByteArray());
        assertThrows(IOException.class,()->codec.write(read.document().replace(0,0,"change",WordTextStyle.DEFAULT),read,new ByteArrayOutputStream()));
    }
    @Test void unrelatedOpaquePartsSurviveSupportedEditing()throws Exception{
        byte[] bytes=write(WordDocument.fromText("old"));OpcPackage archive=OpcPackage.read(bytes,OpcPackage.Limits.DEFAULT);
        byte[] resource={1,2,3,4};var enriched=new ByteArrayOutputStream();archive.withPart("custom/data.bin",resource).write(enriched);
        WordImportResult read=codec.read(new ByteArrayInputStream(enriched.toByteArray()));var edited=read.document().replace(0,3,"new",WordTextStyle.DEFAULT);
        var out=new ByteArrayOutputStream();codec.write(edited,read,out);
        assertArrayEquals(resource,OpcPackage.read(out.toByteArray(),OpcPackage.Limits.DEFAULT).part("custom/data.bin"));
        assertEquals("new",codec.read(new ByteArrayInputStream(out.toByteArray())).document().text());
    }
    @Test void strictWordprocessingNamespaceCanBeReadAndWritten()throws Exception{
        byte[] bytes=patch(write(WordDocument.fromText("strict")),DocxCodec.W,DocxCodec.STRICT);
        WordImportResult read=codec.read(new ByteArrayInputStream(bytes));assertTrue(read.isEditable());
        var out=new ByteArrayOutputStream();codec.write(read.document().replace(0,0,"a",WordTextStyle.DEFAULT),read,out);
        assertEquals(DocxCodec.STRICT,codec.read(new ByteArrayInputStream(out.toByteArray())).namespace());
    }
    @Test void rejectsDtdAndEntityExpansion()throws Exception{
        byte[] bytes=patch(write(WordDocument.fromText("text")),"<w:document","<!DOCTYPE w:document [<!ENTITY leak SYSTEM 'file:///secret'>]><w:document");
        assertThrows(IOException.class,()->codec.read(new ByteArrayInputStream(bytes)));
    }
    @Test void rejectsInvalidXmlCharactersBeforePublishingOutput() {
        assertThrows(IOException.class,()->write(WordDocument.fromText("a\u0000b")));
    }
    @Test void rejectsMacroEnabledPackagesInsteadOfMislabelingThemDocx()throws Exception{
        OpcPackage source=OpcPackage.read(write(WordDocument.fromText("macro")),OpcPackage.Limits.DEFAULT);
        String types=new String(source.part("[Content_Types].xml"),StandardCharsets.UTF_8).replace("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml","application/vnd.ms-word.document.macroEnabled.main+xml");
        var out=new ByteArrayOutputStream();source.withPart("[Content_Types].xml",types.getBytes(StandardCharsets.UTF_8)).write(out);
        assertThrows(IOException.class,()->codec.read(new ByteArrayInputStream(out.toByteArray())));
    }
    @Test void doesNotCloseCallerStreams()throws Exception{
        class Stream extends ByteArrayOutputStream{boolean closed;@Override public void close(){closed=true;}}
        Stream out=new Stream();codec.write(WordDocument.fromText("x"),out);assertFalse(out.closed);
        class Input extends ByteArrayInputStream{boolean closed;Input(byte[] bytes){super(bytes);}@Override public void close(){closed=true;}}
        Input in=new Input(out.toByteArray());codec.read(in);assertFalse(in.closed);
    }
}
