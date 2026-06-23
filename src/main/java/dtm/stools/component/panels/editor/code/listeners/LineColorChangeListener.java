package dtm.stools.component.panels.editor.code.listeners;

import java.awt.Color;

/**
 * Notifica mudanças nas cores de linha do editor (definidas via {@code setLineColor}/
 * {@code setLinesColor} e removidas via {@code removeLineColor}/{@code clearLineColors}).
 */
public interface LineColorChangeListener {

    /**
     * Chamado quando a cor de uma linha é definida (adicionada ou atualizada).
     *
     * @param line       a linha (base 0).
     * @param background  a cor de fundo aplicada; pode ser {@code null}.
     * @param foreground  a cor do texto aplicada; pode ser {@code null}.
     */
    default void onLineColorAdded(int line, Color background, Color foreground) {}

    /**
     * Chamado quando a cor de uma linha é removida.
     *
     * @param line a linha (base 0) que teve a cor removida.
     */
    default void onLineColorRemoved(int line) {}

    /** Chamado quando todas as cores de linha são removidas de uma vez. */
    default void onLineColorsCleared() {}
}
