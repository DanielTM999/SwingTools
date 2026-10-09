package dtm.stools.component.panels.editor.powerpoint.ui;

import dtm.stools.component.panels.editor.powerpoint.model.PptText;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Converts styled Swing text to immutable DrawingML paragraphs, without flattening runs. */
public class PptInlineTextEditor extends JTextPane {
    private static final String SIZE="ppt.logicalSize",BEFORE="ppt.before",AFTER="ppt.after",SPACING="ppt.spacing";
    private final PptText original;
    private double scale;
    public PptInlineTextEditor(PptText text,double scale){
        this.original=text;this.scale=scale;setName("powerpoint.inlineText");setOpaque(false);setBorder(null);
        StyledDocument document=getStyledDocument();
        try{
            for(int i=0;i<text.paragraphs().size();i++){
                var p=text.paragraphs().get(i);int start=document.getLength();
                for(var run:p.runs())document.insertString(document.getLength(),run.text(),attributes(run.style()));
                if(i<text.paragraphs().size()-1)document.insertString(document.getLength(),"\n",attributes(p.runs().isEmpty()?text.firstStyle():p.runs().getLast().style()));
                SimpleAttributeSet paragraph=new SimpleAttributeSet();StyleConstants.setAlignment(paragraph,switch(p.alignment()){case "ctr"->StyleConstants.ALIGN_CENTER;case "r"->StyleConstants.ALIGN_RIGHT;case "just"->StyleConstants.ALIGN_JUSTIFIED;default->StyleConstants.ALIGN_LEFT;});
                paragraph.addAttribute(BEFORE,p.before());paragraph.addAttribute(AFTER,p.after());paragraph.addAttribute(SPACING,p.lineSpacing());
                StyleConstants.setSpaceAbove(paragraph,(float)(p.before()*scale));StyleConstants.setSpaceBelow(paragraph,(float)(p.after()*scale));
                if(p.lineSpacing()<=4)StyleConstants.setLineSpacing(paragraph,(float)(p.lineSpacing()-1));
                document.setParagraphAttributes(start,Math.max(1,document.getLength()-start),paragraph,false);
            }
        }catch(BadLocationException e){throw new IllegalStateException(e);}
        setCaretPosition(0);
        if(document.getLength()==0)setCharacterAttributes(attributes(text.firstStyle()),false);
        javax.swing.undo.UndoManager undo=new javax.swing.undo.UndoManager();document.addUndoableEditListener(undo);
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,java.awt.event.InputEvent.CTRL_DOWN_MASK),"ppt.textUndo");
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y,java.awt.event.InputEvent.CTRL_DOWN_MASK),"ppt.textRedo");
        getActionMap().put("ppt.textUndo",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(undo.canUndo())undo.undo();}});
        getActionMap().put("ppt.textRedo",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){if(undo.canRedo())undo.redo();}});
    }
    private SimpleAttributeSet attributes(PptText.Style s){SimpleAttributeSet a=new SimpleAttributeSet();StyleConstants.setFontFamily(a,s.family());StyleConstants.setFontSize(a,(int)Math.max(1,Math.round(s.size()*scale)));a.addAttribute(SIZE,s.size());StyleConstants.setBold(a,s.bold());StyleConstants.setItalic(a,s.italic());StyleConstants.setUnderline(a,s.underline());StyleConstants.setForeground(a,s.color());return a;}
    private PptText.Style style(AttributeSet a){Object value=a.getAttribute(SIZE);return new PptText.Style(StyleConstants.getFontFamily(a),value instanceof Number n?n.doubleValue():StyleConstants.getFontSize(a)/scale,StyleConstants.isBold(a),StyleConstants.isItalic(a),StyleConstants.isUnderline(a),StyleConstants.getForeground(a));}
    public void format(UnaryOperator<PptText.Style> operation){
        int start=getSelectionStart(),end=getSelectionEnd();StyledDocument document=getStyledDocument();
        if(start==end){setCharacterAttributes(attributes(operation.apply(style(getInputAttributes()))),false);return;}
        for(int at=start;at<end;){Element element=document.getCharacterElement(at);int stop=Math.min(end,element.getEndOffset());document.setCharacterAttributes(at,stop-at,attributes(operation.apply(style(element.getAttributes()))),false);at=stop;}
    }
    public void align(String alignment){SimpleAttributeSet a=new SimpleAttributeSet();StyleConstants.setAlignment(a,switch(alignment){case "ctr"->StyleConstants.ALIGN_CENTER;case "r"->StyleConstants.ALIGN_RIGHT;case "just"->StyleConstants.ALIGN_JUSTIFIED;default->StyleConstants.ALIGN_LEFT;});setParagraphAttributes(a,false);}
    public PptText.Style currentStyle(){return style(getInputAttributes());}
    public void rescale(double value){if(value==scale)return;scale=value;StyledDocument d=getStyledDocument();for(int at=0;at<d.getLength();){Element e=d.getCharacterElement(at);int end=Math.min(d.getLength(),e.getEndOffset());d.setCharacterAttributes(at,end-at,attributes(style(e.getAttributes())),false);at=end;}}
    private double number(AttributeSet a,String key,double fallback){Object value=a.getAttribute(key);return value instanceof Number n?n.doubleValue():fallback;}
    public PptText value(){
        StyledDocument d=getStyledDocument();Element root=d.getDefaultRootElement();List<PptText.Paragraph> paragraphs=new ArrayList<>();
        try{
            for(int i=0;i<root.getElementCount();i++){
                Element p=root.getElement(i);int start=p.getStartOffset(),end=Math.min(d.getLength(),p.getEndOffset());
                if(end>start&&"\n".equals(d.getText(end-1,1)))end--;List<PptText.Run> runs=new ArrayList<>();
                for(int at=start;at<end;){Element r=d.getCharacterElement(at);int stop=Math.min(end,r.getEndOffset());PptText.Style style=style(r.getAttributes());String value=d.getText(at,stop-at);
                    if(!runs.isEmpty()&&runs.getLast().style().equals(style)){var previous=runs.removeLast();runs.add(new PptText.Run(previous.text()+value,style));}else runs.add(new PptText.Run(value,style));at=stop;}
                if(runs.isEmpty())runs.add(new PptText.Run("",style(d.getLength()==0?getInputAttributes():d.getCharacterElement(start).getAttributes())));
                AttributeSet a=p.getAttributes();String alignment=switch(StyleConstants.getAlignment(a)){case StyleConstants.ALIGN_CENTER->"ctr";case StyleConstants.ALIGN_RIGHT->"r";case StyleConstants.ALIGN_JUSTIFIED->"just";default->"l";};
                paragraphs.add(new PptText.Paragraph(runs,alignment,number(a,BEFORE,0),number(a,AFTER,0),number(a,SPACING,1)));
            }
        }catch(BadLocationException e){throw new IllegalStateException(e);}
        return new PptText(paragraphs,original.left(),original.top(),original.right(),original.bottom(),original.anchor());
    }
}
