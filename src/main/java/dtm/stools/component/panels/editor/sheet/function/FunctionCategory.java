package dtm.stools.component.panels.editor.sheet.function;

public enum FunctionCategory {
    MATH("Matemática e trigonometria"), STATISTICAL("Estatística"), LOGICAL("Lógica"), TEXT("Texto"), DATE_TIME("Data e hora"),
    LOOKUP("Pesquisa e referência"), FINANCIAL("Financeira"), ENGINEERING("Engenharia"), INFORMATION("Informações"), DATABASE("Banco de dados"),
    WEB("Web"), CUBE("Cubo"), COMPATIBILITY("Compatibilidade"), ARRAY("Matriz dinâmica"), LAMBDA("Lambda"), GOOGLE("Google Planilhas"), CUSTOM("Personalizada");

    private final String label;
    FunctionCategory(String label) { this.label = label; }
    public String label() { return label; }
}
