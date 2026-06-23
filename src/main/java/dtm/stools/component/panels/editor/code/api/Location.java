package dtm.stools.component.panels.editor.code.api;

/**
 * A pointer to a place in a document.
 * {@code uri} is an opaque identifier understood by the host (file path, URL, logical id).
 * If {@code uri} is null, the location refers to the current editor's document.
 */
public record Location(String uri, Range range) {

    public static Location local(Range range) {
        return new Location(null, range);
    }

    public static Location of(String uri, Range range) {
        return new Location(uri, range);
    }

    public boolean isLocal() {
        return uri == null || uri.isEmpty();
    }
}
