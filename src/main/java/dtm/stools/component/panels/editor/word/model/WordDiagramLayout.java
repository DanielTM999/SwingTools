package dtm.stools.component.panels.editor.word.model;

public enum WordDiagramLayout {
    BASIC_LIST("Lista","Lista básica"),
    VERTICAL_LIST("Lista","Lista vertical"),
    BASIC_PROCESS("Processo","Processo básico"),
    CHEVRON_PROCESS("Processo","Processo em divisas"),
    BASIC_CYCLE("Ciclo","Ciclo básico"),
    HIERARCHY("Hierarquia","Organograma"),
    BASIC_VENN("Relação","Venn básico"),
    BASIC_RADIAL("Relação","Radial básico"),
    BASIC_MATRIX("Matriz","Matriz básica"),
    BASIC_PYRAMID("Pirâmide","Pirâmide básica");

    private final String category;
    private final String displayName;
    WordDiagramLayout(String category, String displayName) { this.category = category; this.displayName = displayName; }
    public String category() { return category; }
    public String displayName() { return displayName; }
    @Override public String toString() { return category + " • " + displayName; }
}
