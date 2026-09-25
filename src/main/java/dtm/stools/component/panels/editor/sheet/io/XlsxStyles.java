package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.format.BuiltinFormats;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetXml;
import dtm.stools.component.panels.editor.sheet.io.ooxml.XmlBuilder;
import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.FillPattern;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetStylePool;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class XlsxStyles {
    private XlsxStyles() {}

    record Font(String name, double size, boolean bold, boolean italic, UnderlineStyle underline, boolean strike, boolean superscript, boolean subscript, Integer color) {}
    record Border(SheetBorder left, SheetBorder right, SheetBorder top, SheetBorder bottom, SheetBorder diagonal, boolean up, boolean down) {}

    static final class ReadResult {
        final List<Integer> xfToStyle = new ArrayList<>();
        final List<DifferentialStyle> dxfs = new ArrayList<>();
        int styleFor(int xf) { return xf >= 0 && xf < xfToStyle.size() ? xfToStyle.get(xf) : 0; }
    }

    static ReadResult read(byte[] data, SheetStylePool pool, SheetTheme theme) throws java.io.IOException {
        ReadResult result = new ReadResult();
        if (data == null) return result;
        Element root = SheetXml.parse(data).getDocumentElement();
        Map<Integer, String> formats = new HashMap<>();
        for (Element nf : SheetXml.children(SheetXml.child(root, "numFmts"), "numFmt")) formats.put(SheetXml.intAttr(nf, "numFmtId", 0), SheetXml.attr(nf, "formatCode"));
        List<Font> fonts = new ArrayList<>();
        for (Element f : SheetXml.children(SheetXml.child(root, "fonts"), "font")) fonts.add(font(f, theme));
        List<SheetFill> fills = new ArrayList<>();
        for (Element f : SheetXml.children(SheetXml.child(root, "fills"), "fill")) fills.add(fill(f, theme));
        List<Border> borders = new ArrayList<>();
        for (Element b : SheetXml.children(SheetXml.child(root, "borders"), "border")) borders.add(border(b, theme));
        for (Element xf : SheetXml.children(SheetXml.child(root, "cellXfs"), "xf")) {
            int numFmt = SheetXml.intAttr(xf, "numFmtId", 0);
            String code = formats.containsKey(numFmt) ? formats.get(numFmt) : Objects.requireNonNullElse(BuiltinFormats.code(numFmt), "General");
            Font font = fonts.isEmpty() ? null : fonts.get(Math.min(fonts.size() - 1, SheetXml.intAttr(xf, "fontId", 0)));
            SheetFill fill = fills.isEmpty() ? SheetFill.NONE : fills.get(Math.min(fills.size() - 1, SheetXml.intAttr(xf, "fillId", 0)));
            Border border = borders.isEmpty() ? null : borders.get(Math.min(borders.size() - 1, SheetXml.intAttr(xf, "borderId", 0)));
            Element al = SheetXml.child(xf, "alignment"), pr = SheetXml.child(xf, "protection");
            CellStyle.CellStyleBuilder b = CellStyle.DEFAULT.toBuilder().numberFormat(code).fill(fill);
            if (font != null) b.fontFamily(font.name()).fontSize(font.size()).bold(font.bold()).italic(font.italic()).underline(font.underline()).strikethrough(font.strike())
                    .superscript(font.superscript()).subscript(font.subscript()).fontColor(font.color());
            if (border != null) b.left(border.left()).right(border.right()).top(border.top()).bottom(border.bottom()).diagonal(border.diagonal()).diagonalUp(border.up()).diagonalDown(border.down());
            if (al != null) {
                b.horizontal(HorizontalAlignment.fromXml(SheetXml.attr(al, "horizontal", "general"))).vertical(VerticalAlignment.fromXml(SheetXml.attr(al, "vertical", "bottom")))
                        .wrap(SheetXml.boolAttr(al, "wrapText", false)).shrinkToFit(SheetXml.boolAttr(al, "shrinkToFit", false)).indent(SheetXml.intAttr(al, "indent", 0));
                int rot = SheetXml.intAttr(al, "textRotation", 0);
                b.rotation(rot == 255 ? 255 : rot > 90 ? 90 - rot : rot);
            }
            if (pr != null) b.locked(SheetXml.boolAttr(pr, "locked", true)).hidden(SheetXml.boolAttr(pr, "hidden", false));
            result.xfToStyle.add(pool.intern(b.build()));
        }
        for (Element dxf : SheetXml.children(SheetXml.child(root, "dxfs"), "dxf")) {
            Element f = SheetXml.child(dxf, "font"), fl = SheetXml.child(dxf, "fill"), nf = SheetXml.child(dxf, "numFmt");
            Integer fontColor = f == null ? null : XlsxColors.read(SheetXml.child(f, "color"), theme);
            Boolean bold = f == null || SheetXml.child(f, "b") == null ? null : SheetXml.boolAttr(SheetXml.child(f, "b"), "val", true);
            Boolean italic = f == null || SheetXml.child(f, "i") == null ? null : SheetXml.boolAttr(SheetXml.child(f, "i"), "val", true);
            Boolean under = f == null || SheetXml.child(f, "u") == null ? null : !"none".equals(SheetXml.attr(SheetXml.child(f, "u"), "val"));
            Boolean strike = f == null || SheetXml.child(f, "strike") == null ? null : SheetXml.boolAttr(SheetXml.child(f, "strike"), "val", true);
            Integer fillColor = null;
            if (fl != null) {
                Element pf = SheetXml.child(fl, "patternFill");
                fillColor = XlsxColors.read(SheetXml.child(pf, "bgColor"), theme);
                if (fillColor == null) fillColor = XlsxColors.read(SheetXml.child(pf, "fgColor"), theme);
            }
            Element b = SheetXml.child(dxf, "border");
            Integer borderColor = b == null ? null : XlsxColors.read(SheetXml.descendant(b, "color"), theme);
            result.dxfs.add(new DifferentialStyle(fontColor, bold, italic, under, strike, fillColor, borderColor, nf == null ? null : SheetXml.attr(nf, "formatCode")));
        }
        return result;
    }

    private static Font font(Element f, SheetTheme theme) {
        Element u = SheetXml.child(f, "u"), va = SheetXml.child(f, "vertAlign");
        String vert = va == null ? "" : SheetXml.attr(va, "val");
        return new Font(SheetXml.attr(SheetXml.child(f, "name"), "val", CellStyle.DEFAULT_FONT), SheetXml.doubleAttr(SheetXml.child(f, "sz"), "val", 11),
                flag(f, "b"), flag(f, "i"), u == null ? UnderlineStyle.NONE : UnderlineStyle.fromXml(SheetXml.attr(u, "val", "single")), flag(f, "strike"),
                vert.equals("superscript"), vert.equals("subscript"), XlsxColors.read(SheetXml.child(f, "color"), theme));
    }

    private static boolean flag(Element parent, String name) {
        Element e = SheetXml.child(parent, name);
        return e != null && SheetXml.boolAttr(e, "val", true);
    }

    private static SheetFill fill(Element f, SheetTheme theme) {
        Element pf = SheetXml.child(f, "patternFill");
        if (pf != null) {
            FillPattern p = FillPattern.fromXml(SheetXml.attr(pf, "patternType", "none"));
            if (p == FillPattern.NONE) return SheetFill.NONE;
            Integer fg = XlsxColors.read(SheetXml.child(pf, "fgColor"), theme), bg = XlsxColors.read(SheetXml.child(pf, "bgColor"), theme);
            if (p == FillPattern.SOLID) return fg != null ? SheetFill.solid(fg) : bg != null ? SheetFill.solid(bg) : SheetFill.NONE;
            return SheetFill.pattern(p, fg, bg);
        }
        Element gf = SheetXml.child(f, "gradientFill");
        if (gf != null) {
            List<Element> stops = SheetXml.children(gf, "stop");
            if (stops.size() >= 2) {
                Integer a = XlsxColors.read(SheetXml.child(stops.getFirst(), "color"), theme), b = XlsxColors.read(SheetXml.child(stops.getLast(), "color"), theme);
                if (a != null && b != null) return SheetFill.gradient(a, b, SheetXml.doubleAttr(gf, "degree", 0));
            }
        }
        return SheetFill.NONE;
    }

    private static Border border(Element b, SheetTheme theme) {
        return new Border(side(SheetXml.child(b, "left"), theme), side(SheetXml.child(b, "right"), theme), side(SheetXml.child(b, "top"), theme),
                side(SheetXml.child(b, "bottom"), theme), side(SheetXml.child(b, "diagonal"), theme), SheetXml.boolAttr(b, "diagonalUp", false), SheetXml.boolAttr(b, "diagonalDown", false));
    }

    private static SheetBorder side(Element e, SheetTheme theme) {
        if (e == null) return SheetBorder.NONE;
        BorderStyle s = BorderStyle.fromXml(SheetXml.attr(e, "style"));
        return SheetBorder.of(s, XlsxColors.read(SheetXml.child(e, "color"), theme));
    }

    static final class WriteResult {
        final Map<Integer, Integer> styleToXf = new HashMap<>();
        final Map<DifferentialStyle, Integer> dxfIndex = new LinkedHashMap<>();
        int xf(int style) { return styleToXf.getOrDefault(style, 0); }
        int dxf(DifferentialStyle d) { return dxfIndex.computeIfAbsent(d, k -> dxfIndex.size()); }
    }

    static byte[] write(SheetStylePool pool, WriteResult result, List<DifferentialStyle> dxfs) {
        Map<String, Integer> numFmts = new LinkedHashMap<>();
        Map<Font, Integer> fonts = new LinkedHashMap<>();
        Map<SheetFill, Integer> fills = new LinkedHashMap<>();
        Map<Border, Integer> borders = new LinkedHashMap<>();
        fonts.put(fontOf(CellStyle.DEFAULT), 0);
        fills.put(SheetFill.NONE, 0);
        fills.put(SheetFill.pattern(FillPattern.GRAY_125, null, null), 1);
        borders.put(borderOf(CellStyle.DEFAULT), 0);
        List<int[]> xfs = new ArrayList<>();
        List<CellStyle> styles = pool.all();
        int nextFmt = 164;
        for (int id = 0; id < styles.size(); id++) {
            CellStyle s = styles.get(id);
            Integer fmt = BuiltinFormats.id(s.numberFormat());
            if (fmt == null) {
                fmt = numFmts.get(s.numberFormat());
                if (fmt == null) { fmt = nextFmt++; numFmts.put(s.numberFormat(), fmt); }
            }
            int font = fonts.computeIfAbsent(fontOf(s), k -> fonts.size());
            int fill = fills.computeIfAbsent(s.fill(), k -> fills.size());
            int border = borders.computeIfAbsent(borderOf(s), k -> borders.size());
            result.styleToXf.put(id, xfs.size());
            xfs.add(new int[]{fmt, font, fill, border, id});
        }
        XmlBuilder x = new XmlBuilder();
        x.open("styleSheet", "xmlns", "http://schemas.openxmlformats.org/spreadsheetml/2006/main");
        if (!numFmts.isEmpty()) {
            x.open("numFmts", "count", String.valueOf(numFmts.size()));
            numFmts.forEach((code, id) -> x.empty("numFmt", "numFmtId", String.valueOf(id), "formatCode", code));
            x.close();
        }
        x.open("fonts", "count", String.valueOf(fonts.size()));
        for (Font f : fonts.keySet()) writeFont(x, f);
        x.close();
        x.open("fills", "count", String.valueOf(fills.size()));
        for (SheetFill f : fills.keySet()) writeFill(x, f);
        x.close();
        x.open("borders", "count", String.valueOf(borders.size()));
        for (Border b : borders.keySet()) writeBorder(x, b);
        x.close();
        x.open("cellStyleXfs", "count", "1").empty("xf", "numFmtId", "0", "fontId", "0", "fillId", "0", "borderId", "0").close();
        x.open("cellXfs", "count", String.valueOf(xfs.size()));
        for (int[] xf : xfs) {
            CellStyle s = styles.get(xf[4]);
            boolean align = s.horizontal() != HorizontalAlignment.GENERAL || s.vertical() != VerticalAlignment.BOTTOM || s.wrap() || s.shrinkToFit() || s.rotation() != 0 || s.indent() > 0;
            boolean protect = !s.locked() || s.hidden();
            x.open("xf", "numFmtId", String.valueOf(xf[0]), "fontId", String.valueOf(xf[1]), "fillId", String.valueOf(xf[2]), "borderId", String.valueOf(xf[3]), "xfId", "0",
                    "applyNumberFormat", xf[0] != 0 ? "1" : null, "applyFont", xf[1] != 0 ? "1" : null, "applyFill", xf[2] != 0 ? "1" : null, "applyBorder", xf[3] != 0 ? "1" : null,
                    "applyAlignment", align ? "1" : null, "applyProtection", protect ? "1" : null);
            if (align) {
                int rot = s.rotation() < 0 && s.rotation() != 255 ? 90 - s.rotation() : s.rotation();
                x.empty("alignment", "horizontal", s.horizontal() == HorizontalAlignment.GENERAL ? null : s.horizontal().xml(), "vertical", s.vertical() == VerticalAlignment.BOTTOM ? null : s.vertical().xml(),
                        "wrapText", s.wrap() ? "1" : null, "shrinkToFit", s.shrinkToFit() ? "1" : null, "textRotation", rot == 0 ? null : String.valueOf(rot), "indent", s.indent() > 0 ? String.valueOf(s.indent()) : null);
            }
            if (protect) x.empty("protection", "locked", s.locked() ? null : "0", "hidden", s.hidden() ? "1" : null);
            x.close();
        }
        x.close();
        x.open("cellStyles", "count", "1").empty("cellStyle", "name", "Normal", "xfId", "0", "builtinId", "0").close();
        x.open("dxfs", "count", String.valueOf(dxfs.size()));
        for (DifferentialStyle d : dxfs) {
            x.open("dxf");
            if (d.fontColor() != null || d.bold() != null || d.italic() != null || d.underline() != null || d.strikethrough() != null) {
                x.open("font");
                if (d.bold() != null) x.empty("b", "val", d.bold() ? null : "0");
                if (d.italic() != null) x.empty("i", "val", d.italic() ? null : "0");
                if (d.strikethrough() != null) x.empty("strike", "val", d.strikethrough() ? null : "0");
                if (d.underline() != null) x.empty("u", "val", d.underline() ? null : "none");
                if (d.fontColor() != null) x.empty("color", "rgb", XlsxColors.hex(d.fontColor()));
                x.close();
            }
            if (d.numberFormat() != null) x.empty("numFmt", "numFmtId", String.valueOf(300 + dxfs.indexOf(d)), "formatCode", d.numberFormat());
            if (d.fillColor() != null) x.open("fill").open("patternFill").empty("bgColor", "rgb", XlsxColors.hex(d.fillColor())).close().close();
            if (d.borderColor() != null) {
                x.open("border");
                for (String side : new String[]{"left", "right", "top", "bottom"}) x.open(side, "style", "thin").empty("color", "rgb", XlsxColors.hex(d.borderColor())).close();
                x.close();
            }
            x.close();
        }
        x.close();
        x.empty("tableStyles", "count", "0", "defaultTableStyle", "TableStyleMedium2", "defaultPivotStyle", "PivotStyleLight16");
        x.close();
        return x.bytes();
    }

    private static Font fontOf(CellStyle s) {
        return new Font(s.fontFamily(), s.fontSize(), s.bold(), s.italic(), s.underline(), s.strikethrough(), s.superscript(), s.subscript(), s.fontColor());
    }

    private static Border borderOf(CellStyle s) {
        return new Border(s.left(), s.right(), s.top(), s.bottom(), s.diagonal(), s.diagonalUp(), s.diagonalDown());
    }

    private static void writeFont(XmlBuilder x, Font f) {
        x.open("font");
        if (f.bold()) x.empty("b");
        if (f.italic()) x.empty("i");
        if (f.strike()) x.empty("strike");
        if (f.underline() != UnderlineStyle.NONE) x.empty("u", "val", f.underline() == UnderlineStyle.SINGLE ? null : f.underline().xml());
        if (f.superscript()) x.empty("vertAlign", "val", "superscript");
        if (f.subscript()) x.empty("vertAlign", "val", "subscript");
        x.empty("sz", "val", trim(f.size()));
        if (f.color() != null) x.empty("color", "rgb", XlsxColors.hex(f.color())); else x.empty("color", "theme", "1");
        x.empty("name", "val", f.name());
        x.empty("family", "val", "2");
        x.close();
    }

    private static void writeFill(XmlBuilder x, SheetFill f) {
        x.open("fill");
        if (f.isGradient()) {
            x.open("gradientFill", "degree", trim(f.gradientAngle()));
            x.open("stop", "position", "0").empty("color", "rgb", XlsxColors.hex(f.foreground())).close();
            x.open("stop", "position", "1").empty("color", "rgb", XlsxColors.hex(f.gradientEnd())).close();
            x.close();
        } else {
            x.open("patternFill", "patternType", f.pattern().xml());
            if (f.foreground() != null) x.empty("fgColor", "rgb", XlsxColors.hex(f.foreground()));
            if (f.background() != null) x.empty("bgColor", "rgb", XlsxColors.hex(f.background()));
            else if (f.pattern() == FillPattern.SOLID) x.empty("bgColor", "indexed", "64");
            x.close();
        }
        x.close();
    }

    private static void writeBorder(XmlBuilder x, Border b) {
        x.open("border", "diagonalUp", b.up() ? "1" : null, "diagonalDown", b.down() ? "1" : null);
        side(x, "left", b.left()); side(x, "right", b.right()); side(x, "top", b.top()); side(x, "bottom", b.bottom()); side(x, "diagonal", b.diagonal());
        x.close();
    }

    private static void side(XmlBuilder x, String name, SheetBorder s) {
        if (!s.visible()) { x.empty(name); return; }
        x.open(name, "style", s.style().xml());
        if (s.color() != null) x.empty("color", "rgb", XlsxColors.hex(s.color())); else x.empty("color", "indexed", "64");
        x.close();
    }

    static String trim(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v); }
}
