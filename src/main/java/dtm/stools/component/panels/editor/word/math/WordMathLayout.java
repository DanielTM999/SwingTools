package dtm.stools.component.panels.editor.word.math;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WordMathLayout {
    public record Box(float width, float ascent, float descent) { public float height() { return ascent + descent; } }
    private interface Node { Box box(); void paint(Graphics2D g, float x, float baseline); }
    private static final FontRenderContext CONTEXT = new FontRenderContext(null,true,true);
    private static volatile String family;

    private WordMathLayout() {}

    public static String fontFamily() {
        String value = family;
        if (value == null) {
            List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            value = installed.contains("Cambria Math") ? "Cambria Math" : installed.contains("STIX Two Math") ? "STIX Two Math" : Font.SERIF;
            family = value;
        }
        return value;
    }
    public static Box measure(WordMath math, float size) { return node(math,size).box(); }
    public static void paint(Graphics2D g, WordMath math, float size, float x, float baseline) { node(math,size).paint(g,x,baseline); }

    private static Node node(WordMath math, float size) {
        return switch (math) {
            case WordMathRow row -> {
                List<Node> items = new ArrayList<>();
                for (int i = 0; i < row.items().size(); i++) {
                    WordMath item = row.items().get(i);
                    boolean unary = item instanceof WordMathText t && t.text().length() == 1 && "+-±".contains(t.text()) && (i == 0 || row.items().get(i-1) instanceof WordMathText p && p.text().length() == 1 && "+-=±×÷<>≤≥≠(".contains(p.text()));
                    items.add(unary ? tight(((WordMathText)item).text(),size) : node(item,size));
                }
                yield row(items);
            }
            case WordMathText text -> text(text.text(),size,text.plain());
            case WordMathFraction f -> fraction(node(f.numerator(),size*0.9f),node(f.denominator(),size*0.9f),size);
            case WordMathScript s -> script(node(s.base(),size),s.subscript() == null ? null : node(s.subscript(),size*0.7f),s.superscript() == null ? null : node(s.superscript(),size*0.7f),size);
            case WordMathRadical r -> radical(r.degree() == null ? null : node(r.degree(),size*0.6f),node(r.body(),size),size);
            case WordMathDelimiter d -> delimiter(d.open(),d.close(),node(d.content(),size),size);
            case WordMathNary n -> nary(n.operator(),n.lower() == null ? null : node(n.lower(),size*0.7f),n.upper() == null ? null : node(n.upper(),size*0.7f),node(n.body(),size),size);
            case WordMathFunction f -> row(List.of(text(f.name(),size,true),space(size*0.15f),node(f.argument(),size)));
            case WordMathAccent a -> accent(a.accent(),node(a.base(),size),size);
            default -> text("?",size,true);
        };
    }

    private static Node tight(String value, float size) {
        TextLayout layout = new TextLayout(value,new Font(fontFamily(),Font.PLAIN,1).deriveFont(size),CONTEXT);
        Box box = new Box(layout.getAdvance(),layout.getAscent()*0.82f,layout.getDescent()*0.8f);
        return new Node() { public Box box() { return box; } public void paint(Graphics2D g, float x, float baseline) { layout.draw(g,x,baseline); } };
    }
    private static Node space(float width) {
        Box box = new Box(width,0,0);
        return new Node() { public Box box() { return box; } public void paint(Graphics2D g, float x, float baseline) {} };
    }

    private static Node text(String value, float size, boolean plain) {
        if (value.isEmpty()) return empty(size);
        List<TextLayout> parts = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            boolean letter = Character.isLetter(value.codePointAt(start));
            int end = start;
            while (end < value.length() && Character.isLetter(value.codePointAt(end)) == letter) end += Character.charCount(value.codePointAt(end));
            int style = !plain && letter ? Font.ITALIC : Font.PLAIN;
            String piece = value.substring(start,end);
            boolean operator = piece.length() == 1 && "+-=<>≤≥≠±×÷→≈∈".indexOf(piece.charAt(0)) >= 0;
            parts.add(new TextLayout(operator ? " " + piece + " " : piece,new Font(fontFamily(),style,1).deriveFont(size),CONTEXT));
            start = end;
        }
        float width = 0, ascent = 0, descent = 0;
        for (TextLayout part : parts) { width += part.getAdvance(); ascent = Math.max(ascent,part.getAscent()*0.82f); descent = Math.max(descent,part.getDescent()*0.8f); }
        Box box = new Box(width,ascent,descent);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) { float cx = x; for (TextLayout part : parts) { part.draw(g,cx,baseline); cx += part.getAdvance(); } }
        };
    }

    private static Node empty(float size) {
        Box box = new Box(size*0.5f,size*0.7f,size*0.1f);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                Graphics2D c = (Graphics2D)g.create();
                try { c.setStroke(new BasicStroke(0.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{1.5f,1.5f},0)); c.draw(new java.awt.geom.Rectangle2D.Float(x+1,baseline-size*0.65f,size*0.4f,size*0.7f)); }
                finally { c.dispose(); }
            }
        };
    }

    private static Node row(List<Node> items) {
        float width = 0, ascent = 0, descent = 0;
        for (Node n : items) { width += n.box().width(); ascent = Math.max(ascent,n.box().ascent()); descent = Math.max(descent,n.box().descent()); }
        Box box = new Box(width,ascent,descent);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) { float cx = x; for (Node n : items) { n.paint(g,cx,baseline); cx += n.box().width(); } }
        };
    }

    private static Node fraction(Node numerator, Node denominator, float size) {
        float axis = size*0.25f, gap = size*0.12f, pad = size*0.12f, rule = Math.max(0.5f,size*0.05f);
        float width = Math.max(numerator.box().width(),denominator.box().width()) + pad*2;
        Box box = new Box(width,axis + gap + numerator.box().height(),denominator.box().height() + gap - axis);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                float bar = baseline - axis;
                numerator.paint(g,x + (width-numerator.box().width())/2,bar - gap - numerator.box().descent());
                denominator.paint(g,x + (width-denominator.box().width())/2,bar + gap + denominator.box().ascent());
                Graphics2D c = (Graphics2D)g.create();
                try { c.setStroke(new BasicStroke(rule)); c.draw(new Line2D.Float(x+pad*0.5f,bar,x+width-pad*0.5f,bar)); } finally { c.dispose(); }
            }
        };
    }

    private static Node script(Node base, Node sub, Node sup, float size) {
        float supShift = Math.max(size*0.38f,base.box().ascent() - (sup == null ? 0 : sup.box().ascent()*0.55f));
        float subShift = Math.max(size*0.18f,base.box().descent() + (sub == null ? 0 : sub.box().ascent()*0.35f));
        float scriptWidth = Math.max(sub == null ? 0 : sub.box().width(),sup == null ? 0 : sup.box().width());
        float ascent = Math.max(base.box().ascent(),sup == null ? 0 : supShift + sup.box().ascent());
        float descent = Math.max(base.box().descent(),sub == null ? 0 : subShift + sub.box().descent());
        Box box = new Box(base.box().width() + scriptWidth + size*0.05f,ascent,descent);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                base.paint(g,x,baseline);
                float sx = x + base.box().width() + size*0.03f;
                if (sup != null) sup.paint(g,sx,baseline - supShift);
                if (sub != null) sub.paint(g,sx,baseline + subShift);
            }
        };
    }

    private static Node radical(Node degree, Node body, float size) {
        float sign = size*0.55f, gap = size*0.12f, rule = Math.max(0.5f,size*0.05f);
        float degreeWidth = degree == null ? 0 : Math.max(0,degree.box().width() - sign*0.4f);
        float ascent = body.box().ascent() + gap + rule, descent = body.box().descent() + size*0.05f;
        if (degree != null) ascent = Math.max(ascent,body.box().height()*0.5f + degree.box().height());
        Box box = new Box(degreeWidth + sign + body.box().width() + size*0.1f,ascent,descent);
        float finalAscent = ascent;
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                float left = x + degreeWidth, top = baseline - body.box().ascent() - gap, bottom = baseline + body.box().descent();
                GeneralPath path = new GeneralPath();
                path.moveTo(left,bottom - (bottom-top)*0.45f);
                path.lineTo(left + sign*0.3f,bottom - (bottom-top)*0.55f);
                path.lineTo(left + sign*0.6f,bottom);
                path.lineTo(left + sign,top);
                path.lineTo(left + sign + body.box().width() + size*0.1f,top);
                Graphics2D c = (Graphics2D)g.create();
                try { c.setStroke(new BasicStroke(rule,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); c.draw(path); } finally { c.dispose(); }
                if (degree != null) degree.paint(g,x,baseline - finalAscent + degree.box().ascent());
                body.paint(g,left + sign + size*0.05f,baseline);
            }
        };
    }

    private static Node delimiter(String open, String close, Node content, float size) {
        Font font = new Font(fontFamily(),Font.PLAIN,1).deriveFont(size);
        TextLayout o = open.isEmpty() ? null : new TextLayout(open,font,CONTEXT), c = close.isEmpty() ? null : new TextLayout(close,font,CONTEXT);
        float glyphHeight = o != null ? o.getAscent()+o.getDescent() : size;
        float needed = Math.max(content.box().height() + size*0.1f,glyphHeight*0.9f);
        float scale = Math.max(1,needed/(glyphHeight*0.9f));
        float ascent = Math.max(content.box().ascent(),size*0.75f) + size*0.05f, descent = Math.max(content.box().descent(),size*0.2f) + size*0.05f;
        float ow = o == null ? 0 : o.getAdvance(), cw = c == null ? 0 : c.getAdvance();
        Box box = new Box(ow + content.box().width() + cw,ascent,descent);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                float center = baseline - (ascent-descent)/2;
                if (o != null) drawScaled(g,o,x,center,scale);
                content.paint(g,x + ow,baseline);
                if (c != null) drawScaled(g,c,x + ow + content.box().width(),center,scale);
            }
        };
    }

    private static void drawScaled(Graphics2D g, TextLayout glyph, float x, float center, float scale) {
        Graphics2D c = (Graphics2D)g.create();
        try {
            float mid = (glyph.getAscent() - glyph.getDescent())/2;
            c.translate(x,center); c.scale(1,scale); glyph.draw(c,0,mid);
        } finally { c.dispose(); }
    }

    private static Node nary(String operator, Node lower, Node upper, Node body, float size) {
        boolean integral = "∫∬∭∮".contains(operator);
        TextLayout glyph = new TextLayout(operator,new Font(fontFamily(),Font.PLAIN,1).deriveFont(size*1.45f),CONTEXT);
        float gw = glyph.getAdvance(), ga = glyph.getAscent()*0.75f, gd = glyph.getDescent()+size*0.05f;
        float limitsWidth = Math.max(lower == null ? 0 : lower.box().width(),upper == null ? 0 : upper.box().width());
        float opWidth = integral ? gw + limitsWidth : Math.max(gw,limitsWidth);
        float ascent = Math.max(body.box().ascent(),ga + (upper == null || integral ? 0 : upper.box().height() + size*0.05f));
        float descent = Math.max(body.box().descent(),gd + (lower == null || integral ? 0 : lower.box().height() + size*0.05f));
        if (integral) { ascent = Math.max(ascent,ga + (upper == null ? 0 : upper.box().ascent()*0.3f)); descent = Math.max(descent,gd + (lower == null ? 0 : lower.box().descent())); }
        Box box = new Box(opWidth + size*0.15f + body.box().width(),ascent,descent);
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                float gx = integral ? x : x + (opWidth-gw)/2, axis = baseline - size*0.25f;
                float glyphBaseline = axis + (glyph.getAscent()-glyph.getDescent())/2;
                glyph.draw(g,gx,glyphBaseline);
                if (integral) {
                    if (upper != null) upper.paint(g,x + gw,glyphBaseline - glyph.getAscent()*0.75f + upper.box().ascent());
                    if (lower != null) lower.paint(g,x + gw*0.7f,glyphBaseline + glyph.getDescent());
                } else {
                    if (upper != null) upper.paint(g,x + (opWidth-upper.box().width())/2,glyphBaseline - glyph.getAscent()*0.75f - size*0.05f - upper.box().descent());
                    if (lower != null) lower.paint(g,x + (opWidth-lower.box().width())/2,glyphBaseline + glyph.getDescent() + size*0.05f + lower.box().ascent());
                }
                body.paint(g,x + opWidth + size*0.15f,baseline);
            }
        };
    }

    private static Node accent(String accent, Node base, float size) {
        float lift = size*0.18f;
        Box box = new Box(base.box().width(),base.box().ascent() + lift + size*0.08f,base.box().descent());
        return new Node() {
            public Box box() { return box; }
            public void paint(Graphics2D g, float x, float baseline) {
                base.paint(g,x,baseline);
                float y = baseline - base.box().ascent() - lift*0.6f, w = base.box().width(), cx = x + w/2;
                Graphics2D c = (Graphics2D)g.create();
                try {
                    c.setStroke(new BasicStroke(Math.max(0.5f,size*0.05f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    switch (accent) {
                        case "hat" -> { GeneralPath p = new GeneralPath(); p.moveTo(cx-size*0.18f,y+size*0.08f); p.lineTo(cx,y-size*0.06f); p.lineTo(cx+size*0.18f,y+size*0.08f); c.draw(p); }
                        case "vec" -> { c.draw(new Line2D.Float(x,y,x+w,y)); c.draw(new Line2D.Float(x+w,y,x+w-size*0.12f,y-size*0.07f)); c.draw(new Line2D.Float(x+w,y,x+w-size*0.12f,y+size*0.07f)); }
                        case "dot" -> c.fill(new java.awt.geom.Ellipse2D.Float(cx-size*0.04f,y-size*0.04f,size*0.08f,size*0.08f));
                        case "tilde" -> { GeneralPath p = new GeneralPath(); p.moveTo(cx-size*0.18f,y+size*0.03f); p.quadTo(cx-size*0.09f,y-size*0.08f,cx,y); p.quadTo(cx+size*0.09f,y+size*0.08f,cx+size*0.18f,y-size*0.03f); c.draw(p); }
                        default -> c.draw(new Line2D.Float(x,y,x+w,y));
                    }
                } finally { c.dispose(); }
            }
        };
    }
}
