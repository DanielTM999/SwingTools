package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordImage(String id, String resourceId, float width, float height, WordCrop crop, float rotation,
                        String altText, WordPlacement placement, boolean lockAspectRatio) implements WordInlineObject {
    public static final String TYPE = "image";
    public WordImage {
        WordInlineObject.requireId(id); Objects.requireNonNull(resourceId);
        WordInlineObject.checkSize(width,height);
        if (width < 1 || height < 1) throw new IllegalArgumentException("Image is too small");
        crop = crop == null ? WordCrop.NONE : crop;
        if (!Float.isFinite(rotation)) throw new IllegalArgumentException("Invalid rotation");
        rotation = ((rotation % 360) + 360) % 360;
        altText = altText == null ? "" : altText;
        placement = placement == null ? WordPlacement.INLINE : placement;
    }
    public static WordImage of(String resourceId, float width, float height) {
        return new WordImage(WordIds.next(),resourceId,width,height,WordCrop.NONE,0,"",WordPlacement.INLINE,true);
    }
    @Override public String type() { return TYPE; }
    @Override public WordImage withId(String value) { return new WordImage(value,resourceId,width,height,crop,rotation,altText,placement,lockAspectRatio); }
    @Override public WordImage resize(float w, float h) { return new WordImage(id,resourceId,w,h,crop,rotation,altText,placement,lockAspectRatio); }
    @Override public WordImage withPlacement(WordPlacement value) { return new WordImage(id,resourceId,width,height,crop,rotation,altText,value,lockAspectRatio); }
    @Override public WordImage withAltText(String value) { return new WordImage(id,resourceId,width,height,crop,rotation,value,placement,lockAspectRatio); }
    @Override public WordImage withRotation(float value) { return new WordImage(id,resourceId,width,height,crop,value,altText,placement,lockAspectRatio); }
    public WordImage withCrop(WordCrop value) { return new WordImage(id,resourceId,width,height,value,rotation,altText,placement,lockAspectRatio); }
    public WordImage withResource(String value) { return new WordImage(id,value,width,height,crop,rotation,altText,placement,lockAspectRatio); }
    public WordImage withLockAspectRatio(boolean value) { return new WordImage(id,resourceId,width,height,crop,rotation,altText,placement,value); }
    @Override public String plainText() { return altText.isBlank() ? "" : "[" + altText + "]"; }
}
