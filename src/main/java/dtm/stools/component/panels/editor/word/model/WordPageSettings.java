package dtm.stools.component.panels.editor.word.model;

public record WordPageSettings(float width, float height, float top, float right, float bottom, float left,
                               int columns, float columnSpacing, float headerDistance, float footerDistance, int pageNumberStart) {
    public static final WordPageSettings A4 = new WordPageSettings(595.276f,841.89f,70.866f,70.866f,70.866f,70.866f);
    public static final WordPageSettings LETTER = new WordPageSettings(612,792,72,72,72,72);
    public WordPageSettings {
        for (float n : new float[]{width,height,top,right,bottom,left,columnSpacing,headerDistance,footerDistance})
            if (!Float.isFinite(n) || n < 0 || n > 14400) throw new IllegalArgumentException("Invalid page geometry");
        if (width-left-right < 36 || height-top-bottom < 36) throw new IllegalArgumentException("Page content area is too small");
        if (columns < 1 || columns > 10 || pageNumberStart < 0) throw new IllegalArgumentException("Invalid section settings");
        if ((width-left-right-columnSpacing*(columns-1))/columns < 24) throw new IllegalArgumentException("Columns are too narrow");
    }
    public WordPageSettings(float width, float height, float top, float right, float bottom, float left) {
        this(width,height,top,right,bottom,left,1,36,35.4f,35.4f,0);
    }
    public float contentWidth() { return width-left-right; }
    public float contentHeight() { return height-top-bottom; }
    public float columnWidth() { return (contentWidth()-columnSpacing*(columns-1))/columns; }
    public boolean landscape() { return width > height; }
    public WordPageSettings withSize(float w, float h) { return new WordPageSettings(w,h,top,right,bottom,left,columns,columnSpacing,headerDistance,footerDistance,pageNumberStart); }
    public WordPageSettings withMargins(float t, float r, float b, float l) { return new WordPageSettings(width,height,t,r,b,l,columns,columnSpacing,headerDistance,footerDistance,pageNumberStart); }
    public WordPageSettings withColumns(int count, float spacing) { return new WordPageSettings(width,height,top,right,bottom,left,count,spacing,headerDistance,footerDistance,pageNumberStart); }
    public WordPageSettings withHeaderFooterDistance(float header, float footer) { return new WordPageSettings(width,height,top,right,bottom,left,columns,columnSpacing,header,footer,pageNumberStart); }
    public WordPageSettings withPageNumberStart(int value) { return new WordPageSettings(width,height,top,right,bottom,left,columns,columnSpacing,headerDistance,footerDistance,value); }
    public WordPageSettings withOrientation(boolean landscapeValue) {
        if (landscapeValue == landscape()) return this;
        return new WordPageSettings(height,width,left,top,right,bottom,columns,columnSpacing,headerDistance,footerDistance,pageNumberStart);
    }
}
