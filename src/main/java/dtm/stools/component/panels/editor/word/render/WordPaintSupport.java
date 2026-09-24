package dtm.stools.component.panels.editor.word.render;

import javax.swing.SwingConstants;
import java.awt.*;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public final class WordPaintSupport {
    public static final int[] PALETTE = {0x4472C4,0xED7D31,0xA5A5A5,0xFFC000,0x5B9BD5,0x70AD47,0x264478,0x9E480E,0x636363,0x997300};
    private WordPaintSupport() {}

    public static Color palette(int index) { return new Color(PALETTE[Math.floorMod(index,PALETTE.length)]); }
    public static Color readable(Color background) {
        double l = 0.2126*background.getRed()/255 + 0.7152*background.getGreen()/255 + 0.0722*background.getBlue()/255;
        return l < 0.55 ? Color.WHITE : new Color(0x1F2937);
    }
    public static Color tint(Color color, float amount) {
        int r = (int)(color.getRed() + (255-color.getRed())*amount), g = (int)(color.getGreen() + (255-color.getGreen())*amount), b = (int)(color.getBlue() + (255-color.getBlue())*amount);
        return new Color(Math.min(255,r),Math.min(255,g),Math.min(255,b));
    }
    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
    }
    public static void text(Graphics2D g, String text, Rectangle2D area, Font font, Color color, int alignment, boolean verticalCenter) {
        if (text == null || text.isBlank() || area.getWidth() < 4 || area.getHeight() < 4) return;
        List<TextLayout> lines = new ArrayList<>();
        float size = font.getSize2D();
        Font current = font;
        for (int attempt = 0; attempt < 6; attempt++) {
            lines = wrap(text,current,(float)area.getWidth(),g);
            float height = 0; for (TextLayout l : lines) height += l.getAscent()+l.getDescent()+l.getLeading();
            if (height <= area.getHeight() || size <= 6) break;
            size *= 0.85f; current = font.deriveFont(size);
        }
        float total = 0; for (TextLayout l : lines) total += l.getAscent()+l.getDescent()+l.getLeading();
        float y = (float)area.getY() + (verticalCenter ? (float)Math.max(0,(area.getHeight()-total)/2) : 0);
        Graphics2D c = (Graphics2D)g.create();
        try {
            c.setColor(color); c.clip(area.getBounds2D().createUnion(new Rectangle2D.Double(area.getX()-1,area.getY()-1,area.getWidth()+2,area.getHeight()+2)));
            for (TextLayout l : lines) {
                y += l.getAscent();
                float advance = l.getVisibleAdvance();
                float x = (float)area.getX() + switch (alignment) { case SwingConstants.CENTER -> (float)(area.getWidth()-advance)/2; case SwingConstants.RIGHT -> (float)(area.getWidth()-advance); default -> 0; };
                l.draw(c,x,y);
                y += l.getDescent()+l.getLeading();
            }
        } finally { c.dispose(); }
    }
    private static List<TextLayout> wrap(String text, Font font, float width, Graphics2D g) {
        List<TextLayout> result = new ArrayList<>();
        for (String paragraph : text.split("\n",-1)) {
            if (paragraph.isEmpty()) { result.add(new TextLayout(" ",font,g.getFontRenderContext())); continue; }
            AttributedString attributed = new AttributedString(paragraph); attributed.addAttribute(TextAttribute.FONT,font);
            LineBreakMeasurer measurer = new LineBreakMeasurer(attributed.getIterator(),g.getFontRenderContext());
            while (measurer.getPosition() < paragraph.length()) result.add(measurer.nextLayout(Math.max(4,width)));
        }
        return result;
    }
    public static void placeholder(Graphics2D g, Rectangle2D bounds, String label) {
        Graphics2D c = (Graphics2D)g.create();
        try {
            c.setColor(new Color(0xF3F4F6)); c.fill(bounds);
            c.setColor(new Color(0x9CA3AF)); c.setStroke(new BasicStroke(0.8f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{3,2},0)); c.draw(bounds);
            text(c,label,new Rectangle2D.Double(bounds.getX()+4,bounds.getY()+2,bounds.getWidth()-8,bounds.getHeight()-4),new Font(Font.SANS_SERIF,Font.PLAIN,9),new Color(0x4B5563),SwingConstants.CENTER,true);
        } finally { c.dispose(); }
    }
}
