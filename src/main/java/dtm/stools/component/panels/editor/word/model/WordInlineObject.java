package dtm.stools.component.panels.editor.word.model;

public interface WordInlineObject {
    String id();
    String type();
    float width();
    float height();
    WordInlineObject withId(String id);
    default WordPlacement placement() { return WordPlacement.INLINE; }
    default String altText() { return ""; }
    default float rotation() { return 0; }
    default boolean resizable() { return true; }
    default boolean lockAspectRatio() { return false; }
    default boolean textual() { return false; }
    default String plainText() { return ""; }
    default WordInlineObject resize(float width, float height) { return this; }
    default WordInlineObject withPlacement(WordPlacement placement) { return this; }
    default WordInlineObject withAltText(String text) { return this; }
    default WordInlineObject withRotation(float degrees) { return this; }
    static void checkSize(float width, float height) {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width < 0 || height < 0 || width > 14400 || height > 14400)
            throw new IllegalArgumentException("Invalid object size");
    }
    static String requireId(String id) {
        if (id == null || id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid object id");
        return id;
    }
}
