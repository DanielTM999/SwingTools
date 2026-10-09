package dtm.stools.component.panels.editor.powerpoint.model;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Immutable styled text. Sizes and margins use presentation logical units. */
public record PptText(List<Paragraph> paragraphs,double left,double top,double right,double bottom,String anchor) {
    public record Style(String family,double size,boolean bold,boolean italic,boolean underline,Color color) {
        public Style {
            family=Objects.requireNonNullElse(family,"Arial");Objects.requireNonNull(color);
            if(!Double.isFinite(size)||size<=0)throw new IllegalArgumentException("Invalid font size");
        }
        public Style size(double value){return new Style(family,value,bold,italic,underline,color);}
        public Style color(Color value){return new Style(family,size,bold,italic,underline,value);}
    }
    public record Run(String text,Style style) {public Run {Objects.requireNonNull(text);Objects.requireNonNull(style);}}
    public record Paragraph(List<Run> runs,String alignment,double before,double after,double lineSpacing) {
        public Paragraph {runs=List.copyOf(runs);alignment=Objects.requireNonNullElse(alignment,"l");}
        public String text(){return runs.stream().map(Run::text).collect(java.util.stream.Collectors.joining());}
    }
    public PptText {
        paragraphs=List.copyOf(paragraphs);anchor=Objects.requireNonNullElse(anchor,"t");
        for(double value:new double[]{left,top,right,bottom})if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid text margin");
    }
    public static PptText plain(String value,double size,Color color){
        Style style=new Style("Arial",size,false,false,false,color);
        return new PptText(java.util.Arrays.stream(value.split("\\n",-1)).map(s->new Paragraph(List.of(new Run(s,style)),"l",0,0,1)).toList(),6,6,6,6,"t");
    }
    public String text(){return paragraphs.stream().map(Paragraph::text).collect(java.util.stream.Collectors.joining("\n"));}
    public Style firstStyle(){return paragraphs.stream().flatMap(p->p.runs().stream()).map(Run::style).findFirst().orElse(new Style("Arial",24,false,false,false,Color.BLACK));}
    public PptText mapStyles(UnaryOperator<Style> change){return new PptText(paragraphs.stream().map(p->new Paragraph(p.runs().stream().map(r->new Run(r.text(),change.apply(r.style()))).toList(),p.alignment(),p.before(),p.after(),p.lineSpacing())).toList(),left,top,right,bottom,anchor);}
    public PptText withText(String value){Style style=firstStyle();return new PptText(java.util.Arrays.stream(value.split("\\n",-1)).map(s->new Paragraph(List.of(new Run(s,style)),paragraphs.isEmpty()?"l":paragraphs.getFirst().alignment(),0,0,1)).toList(),left,top,right,bottom,anchor);}
}
