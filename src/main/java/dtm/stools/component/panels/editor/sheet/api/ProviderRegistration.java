package dtm.stools.component.panels.editor.sheet.api;

@FunctionalInterface
public interface ProviderRegistration extends AutoCloseable {
    @Override void close();

    static ProviderRegistration none() { return () -> {}; }
}
