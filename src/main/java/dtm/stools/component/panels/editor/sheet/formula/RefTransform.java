package dtm.stools.component.panels.editor.sheet.formula;

@FunctionalInterface
public interface RefTransform {
    FormulaNode apply(RefNode ref);
}
