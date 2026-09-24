package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.SwingConstants;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WordChartPainter implements WordObjectPainter {
    private static final Color GRID = new Color(0xD9D9D9), AXIS = new Color(0x595959), TEXT = new Color(0x404040);

    @Override public void paint(Graphics2D graphics, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        if (!(object instanceof WordChart chart)) return;
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            WordPaintSupport.quality(g);
            g.clip(bounds);
            g.setColor(Color.WHITE); g.fill(bounds);
            g.setColor(new Color(0xD0D0D0)); g.setStroke(new BasicStroke(0.6f)); g.draw(new Rectangle2D.Float(bounds.x+0.3f,bounds.y+0.3f,bounds.width-0.6f,bounds.height-0.6f));
            float scale = (float)Math.max(0.55,Math.min(2.2,Math.min(bounds.width/360.0,bounds.height/216.0)));
            Font small = new Font(Font.SANS_SERIF,Font.PLAIN,1).deriveFont(8.5f*scale), titleFont = new Font(Font.SANS_SERIF,Font.BOLD,1).deriveFont(12f*scale);
            Rectangle2D.Float area = new Rectangle2D.Float(bounds.x+8*scale,bounds.y+6*scale,bounds.width-16*scale,bounds.height-12*scale);
            if (!chart.title().isBlank()) {
                float h = titleFont.getSize2D()*1.5f;
                WordPaintSupport.text(g,chart.title(),new Rectangle2D.Float(area.x,area.y,area.width,h),titleFont,TEXT,SwingConstants.CENTER,true);
                area = new Rectangle2D.Float(area.x,area.y+h+2*scale,area.width,area.height-h-2*scale);
            }
            List<String> names = new ArrayList<>(); List<Color> colors = new ArrayList<>();
            if (chart.chartType().isCircular()) for (int i = 0; i < chart.categories().size(); i++) { names.add(chart.categories().get(i)); colors.add(WordPaintSupport.palette(i)); }
            else for (int i = 0; i < chart.series().size(); i++) { names.add(chart.series().get(i).name()); colors.add(color(chart,i)); }
            if (chart.legend() && !names.isEmpty()) area = legend(g,area,names,colors,chart.legendPosition(),small,scale);
            if (area.width < 20 || area.height < 20 || chart.categories().isEmpty() || chart.series().isEmpty()) {
                WordPaintSupport.text(g,"Sem dados",area,small,TEXT,SwingConstants.CENTER,true); return;
            }
            switch (chart.chartType()) {
                case PIE, DOUGHNUT -> pie(g,chart,area,small,scale);
                case RADAR -> radar(g,chart,area,small,scale);
                case SCATTER -> scatter(g,chart,area,small,scale);
                default -> cartesian(g,chart,area,small,scale);
            }
        } finally { g.dispose(); }
    }

    static Color color(WordChart chart, int series) {
        Integer c = chart.series().get(series).color();
        return c == null ? WordPaintSupport.palette(series) : new Color(c);
    }

    private Rectangle2D.Float legend(Graphics2D g, Rectangle2D.Float area, List<String> names, List<Color> colors, WordChart.LegendPosition position, Font font, float scale) {
        FontMetrics fm = g.getFontMetrics(font);
        float swatch = 7*scale, gap = 5*scale, rowH = fm.getHeight();
        boolean vertical = position == WordChart.LegendPosition.LEFT || position == WordChart.LegendPosition.RIGHT;
        g.setFont(font);
        if (vertical) {
            float w = 0; for (String n : names) w = Math.max(w,fm.stringWidth(n)); w = Math.min(area.width*0.35f,w+swatch+gap*2);
            float x = position == WordChart.LegendPosition.RIGHT ? area.x+area.width-w : area.x, y = area.y + Math.max(0,(area.height-rowH*names.size())/2);
            for (int i = 0; i < names.size(); i++) {
                g.setColor(colors.get(i)); g.fill(new Rectangle2D.Float(x+gap,y+(rowH-swatch)/2,swatch,swatch));
                g.setColor(TEXT); g.drawString(clip(names.get(i),fm,(int)(w-swatch-gap*2)),x+gap*1.6f+swatch,y+fm.getAscent()+(rowH-fm.getHeight())/2);
                y += rowH;
            }
            return position == WordChart.LegendPosition.RIGHT ? new Rectangle2D.Float(area.x,area.y,area.width-w,area.height) : new Rectangle2D.Float(area.x+w,area.y,area.width-w,area.height);
        }
        float total = 0; for (String n : names) total += swatch+gap+fm.stringWidth(n)+gap*2;
        int rows = Math.max(1,(int)Math.ceil(total/Math.max(1,area.width)));
        float h = rows*rowH+gap;
        float y = position == WordChart.LegendPosition.TOP ? area.y : area.y+area.height-h+gap/2;
        float x = area.x + Math.max(0,(area.width-Math.min(total,area.width))/2), startX = x;
        for (int i = 0; i < names.size(); i++) {
            float w = swatch+gap+fm.stringWidth(names.get(i))+gap*2;
            if (x+w > area.x+area.width && x > startX) { x = startX; y += rowH; }
            g.setColor(colors.get(i)); g.fill(new Rectangle2D.Float(x,y+(rowH-swatch)/2,swatch,swatch));
            g.setColor(TEXT); g.drawString(names.get(i),x+swatch+gap,y+fm.getAscent());
            x += w;
        }
        return position == WordChart.LegendPosition.TOP ? new Rectangle2D.Float(area.x,area.y+h,area.width,area.height-h) : new Rectangle2D.Float(area.x,area.y,area.width,area.height-h);
    }

    private void cartesian(Graphics2D g, WordChart chart, Rectangle2D.Float area, Font font, float scale) {
        WordChartType type = chart.chartType();
        int categories = chart.categories().size(), seriesCount = chart.series().size();
        boolean stacked = type.isStacked(), percent = type.isPercent(), horizontal = type.isBar();
        double[][] values = new double[seriesCount][categories];
        for (int s = 0; s < seriesCount; s++) for (int c = 0; c < categories; c++) { double v = chart.series().get(s).value(c); values[s][c] = Double.isNaN(v) ? 0 : v; }
        if (percent) for (int c = 0; c < categories; c++) {
            double sum = 0; for (int s = 0; s < seriesCount; s++) sum += Math.abs(values[s][c]);
            for (int s = 0; s < seriesCount; s++) values[s][c] = sum == 0 ? 0 : values[s][c]/sum*100;
        }
        double min = 0, max = 0;
        for (int c = 0; c < categories; c++) {
            double pos = 0, neg = 0;
            for (int s = 0; s < seriesCount; s++) {
                double v = values[s][c];
                if (stacked) { if (v >= 0) pos += v; else neg += v; } else { max = Math.max(max,v); min = Math.min(min,v); }
            }
            if (stacked) { max = Math.max(max,pos); min = Math.min(min,neg); }
        }
        double[] axis = ticks(min,max,5); if (percent) axis = new double[]{min < 0 ? -100 : 0,100,20};
        FontMetrics fm = g.getFontMetrics(font); g.setFont(font);
        float labelWidth = 0;
        for (double v = axis[0]; v <= axis[1]+axis[2]/2; v += axis[2]) labelWidth = Math.max(labelWidth,fm.stringWidth(format(v)+(percent?"%":"")));
        float catLabel = 0; for (String c : chart.categories()) catLabel = Math.max(catLabel,fm.stringWidth(c));
        float left = area.x + (horizontal ? Math.min(area.width*0.3f,catLabel+6*scale) : labelWidth+6*scale) + (chart.valueAxisTitle().isBlank()||horizontal ? 0 : fm.getHeight());
        float bottom = area.y + area.height - fm.getHeight() - 4*scale - (chart.categoryAxisTitle().isBlank() ? 0 : fm.getHeight());
        Rectangle2D.Float plot = new Rectangle2D.Float(left,area.y+4*scale,area.x+area.width-left-4*scale,bottom-area.y-4*scale);
        if (plot.width < 10 || plot.height < 10) return;
        double lo = axis[0], hi = axis[1];
        g.setStroke(new BasicStroke(0.5f));
        for (double v = lo; v <= hi+axis[2]/2; v += axis[2]) {
            float p = (float)((v-lo)/(hi-lo));
            String label = format(v)+(percent?"%":"");
            g.setColor(GRID);
            if (horizontal) {
                float x = plot.x + p*plot.width; g.draw(new Line2D.Float(x,plot.y,x,plot.y+plot.height));
                g.setColor(TEXT); g.drawString(label,x-fm.stringWidth(label)/2f,plot.y+plot.height+fm.getAscent()+2*scale);
            } else {
                float y = plot.y + plot.height - p*plot.height; g.draw(new Line2D.Float(plot.x,y,plot.x+plot.width,y));
                g.setColor(TEXT); g.drawString(label,plot.x-4*scale-fm.stringWidth(label),y+fm.getAscent()/2.5f);
            }
        }
        float zero = (float)((0-lo)/(hi-lo));
        float band = (horizontal ? plot.height : plot.width)/categories;
        for (int c = 0; c < categories; c++) {
            String label = clip(chart.categories().get(c),fm,(int)(horizontal ? plot.x-area.x-4 : Math.max(8,band-2)));
            g.setColor(TEXT);
            if (horizontal) g.drawString(label,plot.x-4*scale-fm.stringWidth(label),plot.y+band*c+band/2+fm.getAscent()/2.5f);
            else g.drawString(label,plot.x+band*c+band/2-fm.stringWidth(label)/2f,plot.y+plot.height+fm.getAscent()+2*scale);
        }
        Shape old = g.getClip(); g.clip(new Rectangle2D.Float(plot.x-1,plot.y-2,plot.width+2,plot.height+4));
        if (type.isColumnOrBar()) {
            double[] pos = new double[categories], neg = new double[categories];
            for (int s = 0; s < seriesCount; s++) {
                Color color = color(chart,s);
                for (int c = 0; c < categories; c++) {
                    double v = values[s][c];
                    double from = stacked ? (v >= 0 ? pos[c] : neg[c]) : 0, to = from + v;
                    if (stacked) { if (v >= 0) pos[c] = to; else neg[c] = to; }
                    float groupWidth = band*0.7f, barWidth = stacked ? groupWidth : groupWidth/seriesCount;
                    float offset = band*0.15f + (stacked ? 0 : s*barWidth);
                    float a = (float)((Math.min(from,to)-lo)/(hi-lo)), b = (float)((Math.max(from,to)-lo)/(hi-lo));
                    Rectangle2D.Float bar = horizontal
                            ? new Rectangle2D.Float(plot.x+a*plot.width,plot.y+band*c+offset,(b-a)*plot.width,barWidth*0.92f)
                            : new Rectangle2D.Float(plot.x+band*c+offset,plot.y+plot.height-b*plot.height,barWidth*0.92f,(b-a)*plot.height);
                    g.setColor(color); g.fill(bar);
                    if (chart.dataLabels() && v != 0) {
                        String label = format(v)+(percent?"%":"");
                        g.setColor(stacked ? WordPaintSupport.readable(color) : TEXT);
                        if (horizontal) g.drawString(label,stacked ? (float)bar.getCenterX()-fm.stringWidth(label)/2f : (float)bar.getMaxX()+2*scale,(float)bar.getCenterY()+fm.getAscent()/2.5f);
                        else g.drawString(label,(float)bar.getCenterX()-fm.stringWidth(label)/2f,stacked ? (float)bar.getCenterY()+fm.getAscent()/2.5f : (float)bar.getY()-2*scale);
                    }
                }
            }
        } else {
            double[] base = new double[categories];
            for (int s = 0; s < seriesCount; s++) {
                Color color = color(chart,s);
                Path2D.Float path = new Path2D.Float(), fill = new Path2D.Float();
                float[] xs = new float[categories], ys = new float[categories];
                for (int c = 0; c < categories; c++) {
                    double v = values[s][c] + (stacked ? base[c] : 0);
                    xs[c] = plot.x + band*c + band/2; ys[c] = plot.y + plot.height - (float)((v-lo)/(hi-lo))*plot.height;
                    if (c == 0) path.moveTo(xs[c],ys[c]); else path.lineTo(xs[c],ys[c]);
                }
                if (type.isArea()) {
                    fill.moveTo(xs[0],ys[0]); for (int c = 1; c < categories; c++) fill.lineTo(xs[c],ys[c]);
                    for (int c = categories-1; c >= 0; c--) fill.lineTo(xs[c],plot.y+plot.height-(float)(((stacked?base[c]:0)-lo)/(hi-lo))*plot.height);
                    fill.closePath();
                    g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),stacked ? 235 : 170)); g.fill(fill);
                }
                g.setColor(color); g.setStroke(new BasicStroke(2*scale,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.draw(path);
                if (type == WordChartType.LINE_MARKERS) for (int c = 0; c < categories; c++) { float r = 3*scale; g.fill(new Ellipse2D.Float(xs[c]-r,ys[c]-r,2*r,2*r)); }
                if (chart.dataLabels()) { g.setColor(TEXT); for (int c = 0; c < categories; c++) { String label = format(values[s][c]); g.drawString(label,xs[c]-fm.stringWidth(label)/2f,ys[c]-4*scale); } }
                if (stacked) for (int c = 0; c < categories; c++) base[c] += values[s][c];
            }
        }
        g.setClip(old);
        g.setColor(AXIS); g.setStroke(new BasicStroke(0.8f));
        if (horizontal) g.draw(new Line2D.Float(plot.x+zero*plot.width,plot.y,plot.x+zero*plot.width,plot.y+plot.height));
        else g.draw(new Line2D.Float(plot.x,plot.y+plot.height-zero*plot.height,plot.x+plot.width,plot.y+plot.height-zero*plot.height));
        axisTitles(g,chart,area,plot,fm,horizontal);
    }

    private void axisTitles(Graphics2D g, WordChart chart, Rectangle2D.Float area, Rectangle2D.Float plot, FontMetrics fm, boolean swap) {
        g.setColor(TEXT);
        String bottom = swap ? chart.valueAxisTitle() : chart.categoryAxisTitle(), side = swap ? chart.categoryAxisTitle() : chart.valueAxisTitle();
        if (!bottom.isBlank()) g.drawString(bottom,plot.x+plot.width/2-fm.stringWidth(bottom)/2f,area.y+area.height-fm.getDescent());
        if (!side.isBlank()) {
            Graphics2D r = (Graphics2D)g.create();
            try { r.translate(area.x+fm.getAscent(),plot.y+plot.height/2+fm.stringWidth(side)/2f); r.rotate(-Math.PI/2); r.drawString(side,0,0); } finally { r.dispose(); }
        }
    }

    private void pie(Graphics2D g, WordChart chart, Rectangle2D.Float area, Font font, float scale) {
        WordChartSeries series = chart.series().getFirst();
        double total = 0; for (int i = 0; i < chart.categories().size(); i++) { double v = series.value(i); if (!Double.isNaN(v) && v > 0) total += v; }
        float d = Math.min(area.width,area.height)*0.92f;
        Rectangle2D.Float circle = new Rectangle2D.Float(area.x+(area.width-d)/2,area.y+(area.height-d)/2,d,d);
        if (total <= 0) { g.setColor(GRID); g.draw(new Ellipse2D.Float(circle.x,circle.y,d,d)); return; }
        double angle = 90;
        FontMetrics fm = g.getFontMetrics(font); g.setFont(font);
        for (int i = 0; i < chart.categories().size(); i++) {
            double v = series.value(i); if (Double.isNaN(v) || v <= 0) continue;
            double extent = -v/total*360;
            Color color = WordPaintSupport.palette(i);
            g.setColor(color); g.fill(new Arc2D.Double(circle,angle,extent,Arc2D.PIE));
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.2f*scale)); g.draw(new Arc2D.Double(circle,angle,extent,Arc2D.PIE));
            if (chart.dataLabels()) {
                double mid = Math.toRadians(angle+extent/2); float r = d*(chart.chartType() == WordChartType.DOUGHNUT ? 0.38f : 0.32f);
                String label = Math.round(v/total*100) + "%";
                g.setColor(WordPaintSupport.readable(color));
                g.drawString(label,(float)(circle.getCenterX()+Math.cos(mid)*r)-fm.stringWidth(label)/2f,(float)(circle.getCenterY()-Math.sin(mid)*r)+fm.getAscent()/2.5f);
            }
            angle += extent;
        }
        if (chart.chartType() == WordChartType.DOUGHNUT) {
            float hole = d*0.5f; g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Float(circle.x+(d-hole)/2,circle.y+(d-hole)/2,hole,hole));
        }
    }

    private void scatter(Graphics2D g, WordChart chart, Rectangle2D.Float area, Font font, float scale) {
        int n = chart.categories().size();
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) { try { xs[i] = Double.parseDouble(chart.categories().get(i).replace(',','.')); } catch (NumberFormatException e) { xs[i] = i+1; } }
        double xmin = Double.MAX_VALUE, xmax = -Double.MAX_VALUE, ymin = 0, ymax = 0;
        for (double x : xs) { xmin = Math.min(xmin,x); xmax = Math.max(xmax,x); }
        for (WordChartSeries s : chart.series()) for (Double v : s.values()) if (!v.isNaN()) { ymin = Math.min(ymin,v); ymax = Math.max(ymax,v); }
        double[] xa = ticks(Math.min(0,xmin),xmax,5), ya = ticks(ymin,ymax,5);
        FontMetrics fm = g.getFontMetrics(font); g.setFont(font);
        float left = area.x + fm.stringWidth(format(ya[1]))+8*scale, bottom = area.y+area.height-fm.getHeight()-4*scale;
        Rectangle2D.Float plot = new Rectangle2D.Float(left,area.y+4*scale,area.x+area.width-left-6*scale,bottom-area.y-4*scale);
        g.setStroke(new BasicStroke(0.5f));
        for (double v = ya[0]; v <= ya[1]+ya[2]/2; v += ya[2]) { float y = plot.y+plot.height-(float)((v-ya[0])/(ya[1]-ya[0]))*plot.height; g.setColor(GRID); g.draw(new Line2D.Float(plot.x,y,plot.x+plot.width,y)); g.setColor(TEXT); g.drawString(format(v),plot.x-4*scale-fm.stringWidth(format(v)),y+fm.getAscent()/2.5f); }
        for (double v = xa[0]; v <= xa[1]+xa[2]/2; v += xa[2]) { float x = plot.x+(float)((v-xa[0])/(xa[1]-xa[0]))*plot.width; g.setColor(GRID); g.draw(new Line2D.Float(x,plot.y,x,plot.y+plot.height)); g.setColor(TEXT); g.drawString(format(v),x-fm.stringWidth(format(v))/2f,plot.y+plot.height+fm.getAscent()+2*scale); }
        for (int s = 0; s < chart.series().size(); s++) {
            g.setColor(color(chart,s));
            for (int i = 0; i < n; i++) {
                double v = chart.series().get(s).value(i); if (Double.isNaN(v)) continue;
                float x = plot.x+(float)((xs[i]-xa[0])/(xa[1]-xa[0]))*plot.width, y = plot.y+plot.height-(float)((v-ya[0])/(ya[1]-ya[0]))*plot.height, r = 3.5f*scale;
                g.fill(new Ellipse2D.Float(x-r,y-r,2*r,2*r));
                if (chart.dataLabels()) g.drawString(format(v),x+r+1,y-r);
            }
        }
        axisTitles(g,chart,area,plot,fm,false);
    }

    private void radar(Graphics2D g, WordChart chart, Rectangle2D.Float area, Font font, float scale) {
        int n = chart.categories().size();
        double max = 0; for (WordChartSeries s : chart.series()) for (Double v : s.values()) if (!v.isNaN()) max = Math.max(max,v);
        double[] axis = ticks(0,max,5);
        FontMetrics fm = g.getFontMetrics(font); g.setFont(font);
        float radius = Math.min(area.width,area.height)/2 - fm.getHeight() - 4*scale;
        float cx = (float)area.getCenterX(), cy = (float)area.getCenterY();
        g.setStroke(new BasicStroke(0.5f));
        for (double v = axis[2]; v <= axis[1]+axis[2]/2; v += axis[2]) {
            Path2D.Float ring = new Path2D.Float(); float r = (float)(v/axis[1])*radius;
            for (int i = 0; i < n; i++) { double a = -Math.PI/2 + 2*Math.PI*i/n; float x = cx+(float)Math.cos(a)*r, y = cy+(float)Math.sin(a)*r; if (i == 0) ring.moveTo(x,y); else ring.lineTo(x,y); }
            ring.closePath(); g.setColor(GRID); g.draw(ring);
        }
        for (int i = 0; i < n; i++) {
            double a = -Math.PI/2 + 2*Math.PI*i/n;
            g.setColor(GRID); g.draw(new Line2D.Float(cx,cy,cx+(float)Math.cos(a)*radius,cy+(float)Math.sin(a)*radius));
            String label = chart.categories().get(i); g.setColor(TEXT);
            g.drawString(label,cx+(float)Math.cos(a)*(radius+fm.getHeight()*0.7f)-fm.stringWidth(label)/2f,cy+(float)Math.sin(a)*(radius+fm.getHeight()*0.7f)+fm.getAscent()/2.5f);
        }
        for (int s = 0; s < chart.series().size(); s++) {
            Path2D.Float shape = new Path2D.Float();
            for (int i = 0; i < n; i++) {
                double v = chart.series().get(s).value(i); if (Double.isNaN(v)) v = 0;
                double a = -Math.PI/2 + 2*Math.PI*i/n; float r = axis[1] == 0 ? 0 : (float)(v/axis[1])*radius;
                float x = cx+(float)Math.cos(a)*r, y = cy+(float)Math.sin(a)*r; if (i == 0) shape.moveTo(x,y); else shape.lineTo(x,y);
            }
            shape.closePath();
            Color color = color(chart,s);
            g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),60)); g.fill(shape);
            g.setColor(color); g.setStroke(new BasicStroke(1.8f*scale)); g.draw(shape);
        }
    }

    static double[] ticks(double min, double max, int count) {
        if (max <= min) max = min + 1;
        double step = nice((max-min)/Math.max(1,count-1),true);
        double lo = Math.floor(min/step)*step, hi = Math.ceil(max/step)*step;
        if (hi <= lo) hi = lo + step;
        return new double[]{lo,hi,step};
    }
    private static double nice(double value, boolean round) {
        double exponent = Math.floor(Math.log10(value)), fraction = value/Math.pow(10,exponent);
        double nice = round ? (fraction < 1.5 ? 1 : fraction < 3 ? 2 : fraction < 7 ? 5 : 10) : (fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10);
        return nice*Math.pow(10,exponent);
    }
    static String format(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.##",DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return format.format(Math.abs(value) < 1e-9 ? 0 : value);
    }
    private static String clip(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t+"…") > width) t = t.substring(0,t.length()-1);
        return t + "…";
    }
}
