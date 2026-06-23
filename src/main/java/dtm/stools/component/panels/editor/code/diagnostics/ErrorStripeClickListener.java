package dtm.stools.component.panels.editor.code.diagnostics;

/**
 * Ouvinte de cliques nos marcadores da barra de erros (error stripe).
 */
@FunctionalInterface
public interface ErrorStripeClickListener {
    void onMarkerClicked(ErrorStripeClickEvent event);
}
