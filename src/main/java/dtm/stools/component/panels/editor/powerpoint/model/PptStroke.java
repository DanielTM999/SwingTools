package dtm.stools.component.panels.editor.powerpoint.model;

import java.awt.Color;
import java.util.Objects;

public record PptStroke(Color color,double width,String dash,String head,String tail) {
    public PptStroke {
        Objects.requireNonNull(color);
        if(!Double.isFinite(width)||width<0)throw new IllegalArgumentException("Invalid line width");
        dash=Objects.requireNonNullElse(dash,"solid");head=Objects.requireNonNullElse(head,"none");tail=Objects.requireNonNullElse(tail,"none");
    }
    public static PptStroke none(){return new PptStroke(new Color(0,0,0,0),0,"solid","none","none");}
}
