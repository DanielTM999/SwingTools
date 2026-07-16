package dtm.stools.component.panels.editor.code.api;

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
