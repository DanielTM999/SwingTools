package dtm.stools.component.panels.editor.sheet.formula;

@FunctionalInterface
public interface NameTransform {
    FormulaNode apply(NameNode name);
}
