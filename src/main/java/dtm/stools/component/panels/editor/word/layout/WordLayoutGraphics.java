package dtm.stools.component.panels.editor.word.layout;

import dtm.stools.component.panels.editor.word.model.WordTabStop;
import java.awt.*;
import java.awt.font.GraphicAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

public final class WordLayoutGraphics {
    private WordLayoutGraphics() {}

    public static final class Space extends GraphicAttribute {
        private final float advance, ascent, descent;
        public Space(float advance, float ascent, float descent) { super(ROMAN_BASELINE); this.advance = advance; this.ascent = ascent; this.descent = descent; }
        @Override public float getAscent() { return ascent; }
        @Override public float getDescent() { return descent; }
        @Override public float getAdvance() { return advance; }
        @Override public void draw(Graphics2D graphics, float x, float y) {}
    }

    public static final class Tab extends GraphicAttribute {
        private final float advance, ascent, descent;
        private final WordTabStop.Leader leader;
        private final Color color;
        public Tab(float advance, float ascent, float descent, WordTabStop.Leader leader, Color color) {
            super(ROMAN_BASELINE); this.advance = Math.max(0.5f,advance); this.ascent = ascent; this.descent = descent; this.leader = leader; this.color = color;
        }
        @Override public float getAscent() { return ascent; }
        @Override public float getDescent() { return descent; }
        @Override public float getAdvance() { return advance; }
        @Override public void draw(Graphics2D graphics, float x, float y) {
            if (leader == null || leader == WordTabStop.Leader.NONE || advance < 6) return;
            Graphics2D g = (Graphics2D)graphics.create();
            try {
                g.setColor(color);
                switch (leader) {
                    case DOT -> { for (float dx = x + 3; dx < x + advance - 3; dx += 3.5f) g.fill(new java.awt.geom.Ellipse2D.Float(dx,y-1.2f,1,1)); }
                    case HYPHEN -> { g.setStroke(new BasicStroke(0.6f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{3,2},0)); g.draw(new Line2D.Float(x+2,y-3,x+advance-2,y-3)); }
                    case UNDERSCORE -> { g.setStroke(new BasicStroke(0.6f)); g.draw(new Line2D.Float(x+1,y+1,x+advance-1,y+1)); }
                    default -> { }
                }
            } finally { g.dispose(); }
        }
    }

    public static final class Label extends GraphicAttribute {
        private final TextLayout layout;
        private final Color color, background;
        private final float rise;
        public Label(String text, Font font, Color color, Color background, boolean superscript) {
            super(ROMAN_BASELINE);
            Font effective = superscript ? font.deriveFont(font.getSize2D()*0.65f) : font;
            this.layout = new TextLayout(text.isEmpty() ? " " : text,effective,WordLayoutEngine.FONT_CONTEXT);
            this.color = color; this.background = background; this.rise = superscript ? font.getSize2D()*0.35f : 0;
        }
        @Override public float getAscent() { return layout.getAscent() + rise; }
        @Override public float getDescent() { return Math.max(0,layout.getDescent() - rise); }
        @Override public float getAdvance() { return layout.getAdvance() + (background == null ? 0 : 4); }
        @Override public void draw(Graphics2D graphics, float x, float y) {
            Graphics2D g = (Graphics2D)graphics.create();
            try {
                if (background != null) {
                    g.setColor(background);
                    g.fill(new RoundRectangle2D.Float(x,y-layout.getAscent()-1,getAdvance(),layout.getAscent()+layout.getDescent()+2,3,3));
                }
                g.setColor(color);
                layout.draw(g,x + (background == null ? 0 : 2),y - rise);
            } finally { g.dispose(); }
        }
    }
}
