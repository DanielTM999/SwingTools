package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.format.ParsedInput;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvCodec {
    public List<List<String>> parse(String text, char delimiter, char quote) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, fieldStarted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == quote) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == quote) { field.append(quote); i++; }
                    else quoted = false;
                } else field.append(c);
                continue;
            }
            if (c == quote && field.isEmpty() && !fieldStarted) { quoted = true; fieldStarted = true; continue; }
            if (c == delimiter) { row.add(field.toString()); field.setLength(0); fieldStarted = false; continue; }
            if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
                continue;
            }
            field.append(c);
            fieldStarted = true;
        }
        if (!field.isEmpty() || !row.isEmpty() || fieldStarted) { row.add(field.toString()); rows.add(row); }
        return rows;
    }

    public CsvDialect detect(byte[] bytes, Locale locale) {
        Charset charset = detectCharset(bytes);
        String text = decode(bytes, charset);
        String sample = text.length() > 65536 ? text.substring(0, 65536) : text;
        char best = ',';
        double bestScore = -1;
        for (char d : new char[]{',', ';', '\t', '|'}) {
            List<List<String>> rows = parse(sample, d, '"');
            if (rows.isEmpty()) continue;
            int n = Math.min(rows.size(), 50);
            int first = rows.getFirst().size();
            if (first < 2) continue;
            int consistent = 0;
            for (int k = 0; k < n; k++) if (rows.get(k).size() == first) consistent++;
            double score = consistent * 10.0 / n + first;
            if (score > bestScore) { bestScore = score; best = d; }
        }
        Locale l = best == ';' ? Locale.forLanguageTag("pt-BR") : best == ',' ? Locale.US : locale;
        boolean bom = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
        return new CsvDialect(best, '"', charset, l, true, bom);
    }

    public static Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) return StandardCharsets.UTF_8;
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) return StandardCharsets.UTF_16LE;
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) return StandardCharsets.UTF_16BE;
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return Charset.forName("windows-1252");
        }
    }

    public static String decode(byte[] bytes, Charset charset) {
        String s = new String(bytes, charset);
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    public SheetImportResult read(byte[] bytes, CsvDialect dialect, String sheetName, boolean date1904) {
        CsvDialect d = dialect == null ? detect(bytes, Locale.getDefault()) : dialect;
        List<List<String>> rows = parse(decode(bytes, d.charset()), d.delimiter(), d.quote());
        SheetWorkbook wb = SheetWorkbook.create(sheetName == null ? "Planilha1" : sheetName);
        SheetWorksheet ws = wb.sheet(0);
        ValueParser parser = new ValueParser(d.locale(), date1904);
        FormulaLocale fl = FormulaLocale.of(d.locale());
        List<String> diagnostics = new ArrayList<>();
        for (int r = 0; r < rows.size() && r < 1_048_576; r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size() && c < 16_384; c++) {
                String text = row.get(c);
                if (text.isEmpty()) continue;
                if (!d.parseValues()) { ws.put(r, c, SheetCell.of(CellValue.of(text))); continue; }
                ParsedInput p = parser.parse(text);
                if (p.isFormula()) {
                    try { ws.put(r, c, SheetCell.formula(Formulas.toCanonical(text, fl))); continue; } catch (RuntimeException e) { ws.put(r, c, SheetCell.of(CellValue.of(text))); continue; }
                }
                int style = p.format() == null ? 0 : wb.styles().intern(wb.style(0).withNumberFormat(p.format()));
                ws.put(r, c, new SheetCell(p.value(), null, style));
            }
        }
        if (rows.size() > 1_048_576) diagnostics.add("O arquivo excede 1.048.576 linhas; o restante foi ignorado.");
        return new SheetImportResult(wb, diagnostics, true, bytes, d.delimiter() == '\t' ? "tsv" : "csv");
    }

    public void write(SheetWorkbook wb, int sheet, CalcEngine engine, CsvDialect dialect, OutputStream out) throws IOException {
        SheetWorksheet ws = wb.sheet(sheet);
        NumberFormatter formatter = new NumberFormatter(dialect.locale(), wb.properties().date1904());
        CellRange used = engine != null ? engine.usedRange(sheet) : ws.usedRange();
        StringBuilder b = new StringBuilder();
        if (dialect.bom() && dialect.charset().equals(StandardCharsets.UTF_8)) b.append('﻿');
        if (used != null) {
            for (int r = 0; r <= used.lastRow(); r++) {
                int last = -1;
                for (int c = 0; c <= used.lastColumn(); c++) if (!value(ws, engine, sheet, r, c).isEmpty()) last = c;
                for (int c = 0; c <= last; c++) {
                    if (c > 0) b.append(dialect.delimiter());
                    CellValue v = value(ws, engine, sheet, r, c);
                    String text = formatter.text(v, wb.style(ws.cell(r, c).style()).numberFormat());
                    b.append(quote(text, dialect));
                }
                b.append("\r\n");
            }
        }
        out.write(b.toString().getBytes(dialect.charset()));
    }

    private static CellValue value(SheetWorksheet ws, CalcEngine engine, int sheet, int r, int c) {
        return engine != null ? engine.valueAt(sheet, r, c) : ws.cell(r, c).value();
    }

    static String quote(String s, CsvDialect d) {
        boolean needs = s.indexOf(d.delimiter()) >= 0 || s.indexOf(d.quote()) >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || !s.equals(s.strip());
        if (!needs) return s;
        String q = String.valueOf(d.quote());
        return q + s.replace(q, q + q) + q;
    }

    public static String tsv(List<List<String>> rows) {
        StringBuilder b = new StringBuilder();
        CsvDialect d = CsvDialect.TAB;
        for (List<String> row : rows) {
            for (int c = 0; c < row.size(); c++) { if (c > 0) b.append('\t'); b.append(quote(row.get(c), d)); }
            b.append("\r\n");
        }
        return b.toString();
    }
}
