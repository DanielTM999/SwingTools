package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.model.WordChart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class XlsxWriter {
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private XlsxWriter() {}

    static String column(int index) {
        StringBuilder b = new StringBuilder(); int n = index + 1;
        while (n > 0) { n--; b.insert(0,(char)('A' + n % 26)); n /= 26; }
        return b.toString();
    }
    static byte[] workbook(WordChart chart) throws IOException {
        StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<worksheet xmlns=\"").append(NS).append("\"><sheetData>");
        sheet.append("<row r=\"1\">");
        for (int s = 0; s < chart.series().size(); s++) cell(sheet,column(s+1)+"1",chart.series().get(s).name());
        sheet.append("</row>");
        for (int c = 0; c < chart.categories().size(); c++) {
            int row = c + 2;
            sheet.append("<row r=\"").append(row).append("\">");
            cell(sheet,"A"+row,chart.categories().get(c));
            for (int s = 0; s < chart.series().size(); s++) {
                double v = chart.series().get(s).value(c);
                if (!Double.isNaN(v)) sheet.append("<c r=\"").append(column(s+1)).append(row).append("\"><v>").append(number(v)).append("</v></c>");
            }
            sheet.append("</row>");
        }
        sheet.append("</sheetData></worksheet>");
        Map<String,byte[]> parts = new LinkedHashMap<>();
        parts.put("[Content_Types].xml",utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>"));
        parts.put("_rels/.rels",utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"));
        parts.put("xl/workbook.xml",utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<workbook xmlns=\""+NS+"\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"));
        parts.put("xl/_rels/workbook.xml.rels",utf8("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"));
        parts.put("xl/worksheets/sheet1.xml",utf8(sheet.toString()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OpcPackage(parts).write(out);
        return out.toByteArray();
    }
    private static void cell(StringBuilder b, String ref, String text) {
        b.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(OoxmlXml.escape(text)).append("</t></is></c>");
    }
    static String number(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return Long.toString((long)v);
        return String.format(Locale.ROOT,"%s",v);
    }
    private static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
}
