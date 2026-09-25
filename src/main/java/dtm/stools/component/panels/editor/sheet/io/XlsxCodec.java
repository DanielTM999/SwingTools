package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class XlsxCodec {
    private final SheetPackage.Limits limits;

    public XlsxCodec() { this(SheetPackage.Limits.DEFAULT); }
    public XlsxCodec(SheetPackage.Limits limits) { this.limits = limits; }

    public SheetImportResult read(byte[] bytes) throws IOException { return XlsxReader.read(bytes, limits); }

    public void write(SheetWorkbook workbook, OutputStream out) throws IOException { write(workbook, null, out); }

    public void write(SheetWorkbook workbook, CalcEngine engine, OutputStream out) throws IOException { new XlsxWriter(workbook, engine).write(out); }

    public byte[] toBytes(SheetWorkbook workbook, CalcEngine engine) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(workbook, engine, out);
        return out.toByteArray();
    }
}
