package dtm.stools.component.panels.editor.sheet.command;

@FunctionalInterface
public interface PointMapper {
    int[] map(int row, int column);
}
