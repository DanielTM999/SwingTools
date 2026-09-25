package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetXml;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import org.w3c.dom.Element;

final class XlsxColors {
    static final int[] INDEXED = {0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFF000000, 0xFFFFFFFF, 0xFFFF0000,
            0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFF800000, 0xFF008000, 0xFF000080, 0xFF808000, 0xFF800080, 0xFF008080, 0xFFC0C0C0,
            0xFF808080, 0xFF9999FF, 0xFF993366, 0xFFFFFFCC, 0xFFCCFFFF, 0xFF660066, 0xFFFF8080, 0xFF0066CC, 0xFFCCCCFF, 0xFF000080, 0xFFFF00FF, 0xFFFFFF00,
            0xFF00FFFF, 0xFF800080, 0xFF800000, 0xFF008080, 0xFF0000FF, 0xFF00CCFF, 0xFFCCFFFF, 0xFFCCFFCC, 0xFFFFFF99, 0xFF99CCFF, 0xFFFF99CC, 0xFFCC99FF,
            0xFFFFCC99, 0xFF3366FF, 0xFF33CCCC, 0xFF99CC00, 0xFFFFCC00, 0xFFFF9900, 0xFFFF6600, 0xFF666699, 0xFF969696, 0xFF003366, 0xFF339966, 0xFF003300,
            0xFF333300, 0xFF993300, 0xFF993366, 0xFF333399, 0xFF333333};

    private XlsxColors() {}

    static Integer read(Element color, SheetTheme theme) {
        if (color == null) return null;
        if (SheetXml.boolAttr(color, "auto", false)) return null;
        Integer base = null;
        String rgb = SheetXml.attr(color, "rgb");
        if (!rgb.isEmpty()) {
            try {
                long v = Long.parseLong(rgb, 16);
                if (rgb.length() <= 6) v |= 0xFF000000L;
                base = (int) v;
                if ((base >>> 24) == 0) base |= 0xFF000000;
            } catch (NumberFormatException ignored) { }
        }
        String th = SheetXml.attr(color, "theme");
        if (!th.isEmpty()) {
            int idx = Integer.parseInt(th);
            int mapped = switch (idx) { case 0 -> 1; case 1 -> 0; case 2 -> 3; case 3 -> 2; default -> idx; };
            base = theme.color(mapped);
        }
        String ix = SheetXml.attr(color, "indexed");
        if (!ix.isEmpty()) {
            int idx = Integer.parseInt(ix);
            if (idx == 64 || idx == 65) return null;
            base = idx >= 0 && idx < INDEXED.length ? INDEXED[idx] : null;
        }
        if (base == null) return null;
        double tint = SheetXml.doubleAttr(color, "tint", 0);
        return tint == 0 ? base : SheetTheme.tint(base, tint);
    }

    static String hex(int argb) { return String.format("%08X", argb); }
}
