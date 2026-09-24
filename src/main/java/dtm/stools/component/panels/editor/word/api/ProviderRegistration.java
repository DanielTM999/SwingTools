package dtm.stools.component.panels.editor.word.api;

@FunctionalInterface
public interface ProviderRegistration extends AutoCloseable {
    @Override void close();
}
