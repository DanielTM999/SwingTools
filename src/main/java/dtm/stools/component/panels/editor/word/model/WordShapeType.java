package dtm.stools.component.panels.editor.word.model;

public enum WordShapeType {
    RECTANGLE("rect","Retângulo"),
    ROUNDED_RECTANGLE("roundRect","Retângulo arredondado"),
    ELLIPSE("ellipse","Elipse"),
    TRIANGLE("triangle","Triângulo"),
    DIAMOND("diamond","Losango"),
    PENTAGON("pentagon","Pentágono"),
    HEXAGON("hexagon","Hexágono"),
    RIGHT_ARROW("rightArrow","Seta para a direita"),
    STAR("star5","Estrela"),
    LINE("line","Linha"),
    CONNECTOR("straightConnector1","Conector reto"),
    ELBOW_CONNECTOR("bentConnector3","Conector angulado"),
    TEXT_BOX("rect","Caixa de texto"),
    GROUP(null,"Grupo");

    private final String preset;
    private final String displayName;
    WordShapeType(String preset, String displayName) { this.preset = preset; this.displayName = displayName; }
    public String preset() { return preset; }
    public String displayName() { return displayName; }
    public boolean isLinear() { return this == LINE || this == CONNECTOR || this == ELBOW_CONNECTOR; }
    public static WordShapeType fromPreset(String preset) {
        for (WordShapeType type : values()) if (type != TEXT_BOX && preset != null && preset.equals(type.preset)) return type;
        return null;
    }
    @Override public String toString() { return displayName; }
}
