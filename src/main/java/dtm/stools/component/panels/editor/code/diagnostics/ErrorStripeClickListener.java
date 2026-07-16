package dtm.stools.component.panels.editor.code.diagnostics;

@FunctionalInterface
public interface ErrorStripeClickListener {
    void onMarkerClicked(ErrorStripeClickEvent event);
}
