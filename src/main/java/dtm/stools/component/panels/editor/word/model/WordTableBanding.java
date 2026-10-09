package dtm.stools.component.panels.editor.word.model;

public record WordTableBanding(Integer firstColor,Integer secondColor,int rowsPerBand,boolean preserveHeaderRows,int firstRow,int lastRow) {
    public WordTableBanding(Integer firstColor,Integer secondColor,int rowsPerBand,boolean preserveHeaderRows) {
        this(firstColor,secondColor,rowsPerBand,preserveHeaderRows,0,Integer.MAX_VALUE);
    }
    public WordTableBanding {
        if(rowsPerBand<1||rowsPerBand>100)throw new IllegalArgumentException("Invalid row band size");
        if(firstRow<0||lastRow<firstRow)throw new IllegalArgumentException("A linha final deve ser maior ou igual à linha inicial.");
        if(firstColor!=null)firstColor&=0xffffff;
        if(secondColor!=null)secondColor&=0xffffff;
    }
}
