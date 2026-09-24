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
            if (paintSpecific(g,id)) return;
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
            } else if (id.equals("spacing") || id.startsWith("spacing.")) {
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
            } else if(id.equals("diagram") || id.equals("insert.diagram") || id.equals("arrange")) {
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
    private static boolean paintSpecific(Graphics2D g,String id) {
        Color ink=g.getColor();
        switch(id) {
            case "list.continue" -> {text(g,"1",2,9,8);text(g,"2",2,21,8);line(g,9,5,21,5);line(g,9,18,21,18);g.setColor(UiTokens.accent());arrow(g,9,11,19,11);}
            case "insert.pagebreak" -> {
                line(g,4,2,4,9);line(g,4,9,20,9);line(g,20,9,20,2);
                line(g,4,22,4,15);line(g,4,15,20,15);line(g,20,15,20,22);
                g.setColor(UiTokens.accent());for(int x=2;x<23;x+=4)line(g,x,12,x+1,12);
            }
            case "insert.columnbreak" -> {
                for(int y=4;y<=20;y+=4){line(g,2,y,8,y);line(g,16,y,22,y);}
                g.setColor(UiTokens.accent());arrow(g,8,12,16,12);
            }
            case "insert.sectionbreak" -> {
                line(g,3,3,21,3);line(g,3,7,15,7);line(g,3,18,21,18);line(g,3,22,15,22);
                g.setColor(UiTokens.accent());line(g,2,11,22,11);line(g,2,14,22,14);
            }
            case "insert.field.page" -> {page(g);g.setColor(UiTokens.accent());text(g,"#",7,17,15);}
            case "insert.field.date", "insert.form.date" -> {
                calendar(g);
                if(id.equals("insert.form.date")){g.setColor(UiTokens.accent());line(g,14,15,17,18);line(g,17,18,20,15);}
                else {g.setColor(UiTokens.accent());g.fillRect(7,11,3,3);g.fillRect(12,11,3,3);g.fillRect(7,16,3,3);}
            }
            case "insert.form.text" -> {g.drawRoundRect(2,5,20,14,2,2);text(g,"ab",4,15,10);g.setColor(UiTokens.accent());line(g,17,8,17,16);line(g,15,8,19,8);line(g,15,16,19,16);}
            case "insert.form.checkbox" -> {g.drawRoundRect(3,3,18,18,2,2);g.setColor(UiTokens.accent());check(g,6,12);}
            case "insert.form.dropdown" -> {g.drawRoundRect(2,5,20,14,2,2);line(g,5,12,10,12);line(g,13,5,13,19);g.setColor(UiTokens.accent());line(g,15,10,18,14);line(g,18,14,21,10);}
            case "block.save", "block.insert" -> {
                g.drawRect(2,2,8,8);g.drawRect(14,2,8,8);g.drawRect(2,14,8,8);g.setColor(UiTokens.accent());
                if(id.equals("block.insert")){line(g,14,18,22,18);line(g,18,14,18,22);}
                else {g.drawRect(13,13,9,9);line(g,16,13,16,16);line(g,16,16,20,16);g.drawRect(16,19,3,3);}
            }
            case "insert.bookmark" -> {g.draw(new Path2D.Double(){{moveTo(6,2);lineTo(18,2);lineTo(18,22);lineTo(12,17);lineTo(6,22);closePath();}});g.setColor(UiTokens.accent());line(g,9,7,15,7);}
            case "insert.crossref" -> {g.drawRect(2,2,9,12);g.drawRect(13,10,9,12);g.setColor(UiTokens.accent());arrow(g,8,6,18,6);line(g,18,6,18,9);}
            case "link.remove" -> {g.drawArc(2,7,10,10,60,270);g.drawArc(12,7,10,10,240,270);g.setColor(UiTokens.accent());line(g,9,3,15,21);}
            case "insert.textbox", "textbox" -> {g.drawRect(2,4,20,16);text(g,"T",7,17,15);g.setColor(UiTokens.accent());for(int x:new int[]{2,22})for(int y:new int[]{4,20})g.fillRect(x-1,y-1,3,3);}
            case "page.portrait", "page.landscape" -> {
                if(id.endsWith("landscape")){g.drawRect(2,5,20,14);line(g,6,10,18,10);line(g,6,14,14,14);}
                else {page(g);line(g,8,8,16,8);line(g,8,12,14,12);}
            }
            case "page.size", "page.a4", "page.a5", "page.letter", "page.legal" -> {
                g.drawRect(6,2,16,16);g.setColor(UiTokens.accent());line(g,2,2,2,18);line(g,1,2,3,2);line(g,1,18,3,18);line(g,6,22,22,22);line(g,6,21,6,23);line(g,22,21,22,23);
            }
            case "page.setup", "margins", "margins.normal", "margins.narrow", "margins.wide" -> {
                page(g);g.setColor(UiTokens.accent());int margin=id.endsWith("narrow")?2:id.endsWith("wide")?5:3;
                g.drawRect(5+margin,6,14-2*margin,12);
                if(id.equals("page.setup")){line(g,2,8,8,8);line(g,16,16,22,16);}
            }
            case "page.columns.1", "page.columns.2", "page.columns.3" -> {
                int count=id.charAt(id.length()-1)-'0',width=18/count;
                for(int col=0;col<count;col++)for(int y=4;y<=20;y+=4)line(g,3+col*width,y,3+col*width+width-3,y);
            }
            case "header", "footer" -> {page(g);g.setColor(UiTokens.accent());g.fillRect(7,id.equals("header")?5:16,10,3);}
            case "toc" -> {for(int y=5;y<=19;y+=7){line(g,2,y,11,y);g.fillOval(14,y-1,1,1);g.fillOval(17,y-1,1,1);text(g,Integer.toString((y-5)/7+1),19,y+2,7);}}
            case "footnote", "endnote" -> {page(g);text(g,id.equals("footnote")?"1":"i",13,8,7);line(g,8,11,16,11);g.setColor(UiTokens.accent());line(g,8,id.equals("footnote")?16:19,16,id.equals("footnote")?16:19);}
            case "caption" -> {g.drawRect(3,2,18,13);g.setColor(UiTokens.accent());line(g,5,19,19,19);line(g,8,22,16,22);}
            case "styles", "style.new", "style.update" -> {text(g,"A",2,18,19);g.setColor(UiTokens.accent());line(g,15,5,22,5);line(g,17,10,22,10);line(g,18,15,22,15);}
            case "track" -> {line(g,2,5,12,5);line(g,2,11,9,11);line(g,2,17,7,17);g.setColor(UiTokens.accent());pencil(g);}
            case "author" -> {g.drawOval(8,2,8,8);g.drawArc(3,12,18,18,0,180);line(g,3,21,21,21);}
            case "change.accept", "change.acceptAll", "change.reject", "change.rejectAll", "change.next" -> {
                if(id.endsWith("All")){g.drawRect(2,2,14,17);g.drawRect(6,5,16,17);}
                else page(g);
                g.setColor(UiTokens.accent());
                if(id.contains("accept"))check(g,7,13);
                else if(id.contains("reject")){line(g,8,9,16,17);line(g,8,17,16,9);}
                else arrow(g,7,12,20,12);
            }
            case "compare" -> {g.drawRect(1,3,9,18);g.drawRect(14,3,9,18);g.setColor(UiTokens.accent());line(g,3,8,8,8);line(g,16,15,21,15);arrow(g,7,12,17,12);}
            case "view.pages" -> {g.drawRect(3,2,18,8);g.drawRect(3,14,18,8);line(g,7,6,17,6);line(g,7,18,17,18);}
            case "view.continuous" -> {g.drawRect(3,2,18,20);for(int y=6;y<22;y+=4)line(g,7,y,17,y);}
            case "focus" -> {line(g,2,8,2,2);line(g,2,2,8,2);line(g,16,2,22,2);line(g,22,2,22,8);line(g,2,16,2,22);line(g,2,22,8,22);line(g,16,22,22,22);line(g,22,22,22,16);}
            case "new" -> {page(g);g.setColor(UiTokens.accent());line(g,8,12,16,12);line(g,12,8,12,16);}
            case "print" -> {g.drawRect(6,2,12,6);g.drawRoundRect(2,8,20,10,2,2);g.setColor(UiTokens.surface());g.fillRect(6,14,12,8);g.setColor(ink);g.drawRect(6,14,12,8);line(g,17,11,19,11);}
            case "export.html", "export.text", "export.pdf" -> {
                page(g);g.setColor(UiTokens.accent());text(g,id.endsWith("html")?"<>":id.endsWith("pdf")?"PDF":"TXT",6,11,id.endsWith("html")?10:7);arrow(g,10,17,22,17);
            }
            case "versions", "recover" -> {g.draw(new Arc2D.Double(3,3,18,18,40,290,Arc2D.OPEN));line(g,2,3,2,9);line(g,2,9,8,9);g.setColor(UiTokens.accent());if(id.equals("versions")){line(g,12,6,12,12);line(g,12,12,16,15);}else arrow(g,12,18,12,7);}
            case "template" -> {page(g);g.setColor(UiTokens.accent());text(g,"{ }",6,16,12);}
            case "mailmerge" -> {g.drawRect(2,7,20,14);line(g,2,7,12,15);line(g,12,15,22,7);g.setColor(UiTokens.accent());line(g,5,2,19,2);line(g,8,4,16,4);}
            case "object.properties", "table.properties" -> {for(int y=5;y<=19;y+=7)line(g,2,y,22,y);g.setColor(UiTokens.accent());g.fillRoundRect(6,2,3,6,2,2);g.fillRoundRect(15,9,3,6,2,2);g.fillRoundRect(9,16,3,6,2,2);}
            case "object.delete" -> {line(g,3,5,21,5);line(g,9,2,15,2);g.drawRect(5,5,14,17);line(g,10,9,10,18);line(g,14,9,14,18);}
            case "object.rotate" -> {g.drawRect(5,10,11,11);g.setColor(UiTokens.accent());g.drawArc(6,2,16,16,0,150);line(g,22,5,22,10);line(g,17,10,22,10);}
            case "object.alt" -> {g.drawRoundRect(2,3,20,18,2,2);text(g,"Aa",4,16,12);}
            case "object.forward", "object.backward", "shape.group", "shape.ungroup" -> {
                g.drawRect(2,3,13,13);g.setColor(UiTokens.surface());g.fillRect(9,10,13,12);g.setColor(UiTokens.accent());g.drawRect(9,10,13,12);
                if(id.equals("object.forward"))arrow(g,18,8,18,2);
                else if(id.equals("object.backward"))arrow(g,5,17,5,23);
                else if(id.equals("shape.group")){line(g,1,1,23,1);line(g,23,1,23,23);}
                else {line(g,2,19,7,19);line(g,18,5,23,5);}
            }
            case "object.wrap.inline", "object.wrap.square", "object.wrap.tight", "object.wrap.topbottom", "object.wrap.behind", "object.wrap.front" -> {
                boolean around=id.endsWith("square")||id.endsWith("tight"),inline=id.endsWith("inline");
                for(int y=3;y<=21;y+=4){
                    if(y>=7&&y<=15){if(around){line(g,2,y,5,y);line(g,19,y,22,y);}else if(inline||id.endsWith("behind"))line(g,2,y,22,y);}
                    else line(g,2,y,22,y);
                }
                g.setColor(UiTokens.accent());if(id.endsWith("front"))g.fillRect(7,6,10,11);else g.drawRect(7,6,10,11);
            }
            default -> {return false;}
        }
        return true;
    }
    private static void page(Graphics2D g){g.drawRoundRect(5,2,14,20,2,2);}
    private static void calendar(Graphics2D g){g.drawRoundRect(2,4,20,18,2,2);line(g,2,9,22,9);line(g,7,2,7,6);line(g,17,2,17,6);}
    private static void text(Graphics2D g,String text,int x,int y,int size){g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size));g.drawString(text,x,y);}
    private static void check(Graphics2D g,int x,int y){line(g,x,y,x+4,y+4);line(g,x+4,y+4,x+11,y-5);}
    private static void pencil(Graphics2D g){g.draw(new Path2D.Double(){{moveTo(9,21);lineTo(11,15);lineTo(19,3);lineTo(23,6);lineTo(15,18);closePath();}});}
    private static void arrow(Graphics2D g,int x1,int y1,int x2,int y2){
        line(g,x1,y1,x2,y2);
        if(y1==y2){int back=x2>x1?-3:3;line(g,x2+back,y2-3,x2,y2);line(g,x2+back,y2+3,x2,y2);}
        else {int back=y2>y1?-3:3;line(g,x2-3,y2+back,x2,y2);line(g,x2+3,y2+back,x2,y2);}
    }
    private static void line(Graphics2D g, int x, int y, int x2, int y2) { g.drawLine(x,y,x2,y2); }
}
