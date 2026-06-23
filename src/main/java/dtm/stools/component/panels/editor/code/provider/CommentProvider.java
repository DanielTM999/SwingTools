package dtm.stools.component.panels.editor.code.provider;


public interface CommentProvider extends CodeEditorProvider {

    String lineCommentPrefix();

    default String[] blockCommentDelimiters() {
        return null;
    }

    static CommentProvider line(String prefix) {
        return () -> prefix;
    }

    static CommentProvider of(String lineComment, String blockStart, String blockEnd) {
        return new CommentProvider() {
            @Override public String lineCommentPrefix() { return lineComment; }
            @Override public String[] blockCommentDelimiters() {
                return blockStart == null || blockEnd == null ? null
                        : new String[]{blockStart, blockEnd};
            }
        };
    }
}
