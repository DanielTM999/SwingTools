package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.configs.UiTokens;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Small vector command illustrations, painted in the current theme at any display scale. */
public record WordIcon(String command, int size) implements Icon {
    @Override public int getIconWidth() { return UIScale.scale(size); }
    @Override public int getIconHeight() { return UIScale.scale(size); }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y); g.scale(getIconWidth()/24.0, getIconHeight()/24.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c.isEnabled() ? UiTokens.foreground() : UiTokens.muted());
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            String id = command.replace("word.", "");
            if (id.equals("close")) { line(g,7,7,17,17); line(g,7,17,17,7); }
            else if (id.equals("bold") || id.equals("italic") || id.equals("underline") || id.equals("strike") || id.equals("superscript") || id.equals("subscript")) {
                g.setFont(new Font(Font.SANS_SERIF, id.equals("bold") ? Font.BOLD : id.equals("italic") ? Font.ITALIC : Font.PLAIN, 18));
                g.drawString(switch(id) { case "bold" -> "N"; case "italic" -> "I"; case "underline" -> "S"; case "strike" -> "a"; default -> "x"; }, 5, 18);
                if(id.equals("underline")) line(g,4,21,19,21);
                if(id.equals("strike")) line(g,3,12,21,12);
                if(id.endsWith("script")) { g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10)); g.drawString("2",15,id.equals("superscript") ? 9 : 23); }
            } else if (id.contains("indent")) {
                for(int i=0;i<4;i++)line(g,11,5+i*5,21,5+i*5);
                if(id.endsWith("less")){line(g,8,9,3,12);line(g,3,12,8,15);}else{line(g,3,9,8,12);line(g,8,12,3,15);}
            } else if (id.equals("spacing")) {
                line(g,4,3,4,21);line(g,1,6,4,3);line(g,4,3,7,6);line(g,1,18,4,21);line(g,4,21,7,18);
                for(int i=0;i<4;i++)line(g,10,5+i*5,22,5+i*5);
            } else if (id.startsWith("align.") || id.equals("bullets") || id.equals("numbering")) {
                for(int i=0;i<4;i++) { int w = i%2==0 || id.endsWith("JUSTIFY") ? 17 : 12;
                    int left = id.endsWith("RIGHT") ? 21-w : id.endsWith("CENTER") ? (24-w)/2 : 3;
                    if(id.equals("bullets") || id.equals("numbering")) {
                        if(id.equals("bullets"))g.fillOval(2,4+i*5,2,2);
                        else{g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,6));g.drawString(Integer.toString(i+1),1,7+i*5);}
                        left=7; w=14;
                    }
                    line(g,left,5+i*5,left+w,5+i*5);
                }
            } else if (id.equals("color") || id.equals("highlight") || id.equals("shading") || id.contains("fill")) {
                if(id.equals("color")){g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,17));g.drawString("A",5,16);}
                else {g.draw(new Path2D.Double(){{moveTo(6,14);lineTo(13,3);lineTo(20,7);lineTo(13,18);closePath();}});line(g,6,14,13,18);}
                g.setColor(id.equals("highlight") ? new Color(0xE4B52A) : UiTokens.accent());g.fillRoundRect(3,19,18,4,2,2);
            } else if(id.equals("palette")) {
                g.drawRoundRect(2,4,20,16,3,3);for(int i=0;i<3;i++){g.drawRect(5,7+i*4,1,1);line(g,10,8+i*4,19,8+i*4);}
            } else if(id.contains("find") || id.startsWith("zoom")) {
                g.drawOval(3,3,12,12); line(g,14,14,21,21);
                if(id.startsWith("zoom"))line(g,6,9,12,9);if(id.endsWith(".in"))line(g,9,6,9,12);
            } else if(id.equals("copy")) {
                g.drawRoundRect(7,6,14,16,2,2);line(g,3,18,3,2);line(g,3,2,16,2);line(g,10,11,18,11);line(g,10,15,18,15);
            } else if(id.equals("clearFormat")) {
                g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.drawString("A",2,15);
                g.setColor(UiTokens.accent());g.draw(new Path2D.Double(){{moveTo(10,18);lineTo(17,10);lineTo(22,14);lineTo(16,21);lineTo(13,21);closePath();}});
            } else if(id.contains("link")) {
                g.rotate(-Math.PI/4,12,12);g.drawRoundRect(2,8,12,8,6,6);g.drawRoundRect(10,8,12,8,6,6);
            } else if(id.contains("comment")) {
                g.draw(new Path2D.Double(){{moveTo(3,3);lineTo(21,3);lineTo(21,16);lineTo(10,16);lineTo(5,21);lineTo(5,16);lineTo(3,16);closePath();}});
                line(g,7,7,17,7);line(g,7,11,15,11);
            } else if(id.contains("equation")) {
                g.setFont(new Font(Font.SERIF,Font.ITALIC,21));g.drawString("π",3,18);
            } else if(id.equals("shape") || id.contains("textbox")) {
                g.drawRect(2,3,13,12);g.setColor(UiTokens.accent());g.drawOval(9,10,13,12);
            } else if(id.equals("diagram") || id.equals("arrange")) {
                g.drawRoundRect(7,2,10,6,2,2);line(g,12,8,12,12);line(g,5,12,19,12);line(g,5,12,5,16);line(g,19,12,19,16);
                g.setColor(UiTokens.accent());g.drawRect(1,16,8,6);g.drawRect(15,16,8,6);
            } else if(id.equals("list.restart")) {
                g.draw(new Arc2D.Double(3,3,18,18,-80,280,Arc2D.OPEN));line(g,2,3,2,9);line(g,2,9,8,9);
                g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));g.drawString("1",9,17);
            } else if(id.equals("undo") || id.equals("redo")) {
                if(id.equals("redo")) { g.translate(24,0);g.scale(-1,1); }
                g.draw(new Arc2D.Double(5,6,15,14,-80,230,Arc2D.OPEN));line(g,4,4,4,11);line(g,4,11,11,11);
            } else if(id.contains("save")) {
                g.drawRoundRect(3,3,18,18,2,2);g.drawRect(7,3,10,6);g.drawRect(7,14,10,7);
            } else if(id.contains("paste")) {
                g.drawRoundRect(5,4,15,18,2,2);g.setColor(UiTokens.surface());g.fillRect(9,2,7,5);g.setColor(UiTokens.accent());g.drawRoundRect(8,2,8,5,2,2);line(g,9,12,17,12);line(g,9,16,17,16);
            } else if(id.contains("cut")) {
                g.drawOval(2,15,6,6);g.drawOval(15,15,6,6);line(g,6,16,18,3);line(g,17,16,5,3);
            } else if(id.contains("table")) {
                g.drawRoundRect(2,3,20,18,2,2);line(g,2,9,22,9);line(g,2,15,22,15);line(g,9,3,9,21);line(g,16,3,16,21);
            } else if(id.contains("image")) {
                g.drawRoundRect(2,3,20,18,2,2);g.drawOval(6,6,4,4);g.setColor(UiTokens.accent());line(g,3,19,11,11);line(g,11,11,16,16);line(g,16,16,20,12);
            } else if(id.contains("chart")) {
                line(g,3,3,3,21);line(g,3,21,22,21);g.setColor(UiTokens.accent());g.fillRect(6,12,3,7);g.fillRect(12,5,3,14);g.fillRect(18,9,3,10);
            } else if(id.contains("navigation") || id.contains("panel")) {
                g.drawRoundRect(2,3,20,18,2,2);line(g,9,3,9,21);for(int i=0;i<3;i++)line(g,4,7+i*4,7,7+i*4);
            } else if(id.contains("open")) {
                g.draw(new Path2D.Double() {{ moveTo(2,19);lineTo(2,5);lineTo(9,5);lineTo(12,8);lineTo(21,8);lineTo(21,19);closePath(); }});line(g,3,11,21,11);
            } else {
                g.drawRoundRect(5,2,14,20,2,2);g.setColor(UiTokens.accent());line(g,8,8,16,8);line(g,8,12,16,12);line(g,8,16,13,16);
            }
        } finally { g.dispose(); }
    }
    private static void line(Graphics2D g, int x, int y, int x2, int y2) { g.drawLine(x,y,x2,y2); }
}
