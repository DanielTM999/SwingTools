package dtm.stools.component.panels.editor.word.model;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record WordCustomObject(String id, String customType, Map<String,String> data, float width, float height, String altText,
                               WordPlacement placement, String previewResourceId) implements WordInlineObject {
    private static final Pattern TYPE = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)+");
    public WordCustomObject {
        WordInlineObject.requireId(id); Objects.requireNonNull(customType);
        if (!TYPE.matcher(customType).matches() || customType.length() > 128) throw new IllegalArgumentException("Custom types must be namespaced, e.g. app.signature");
        TreeMap<String,String> copy = new TreeMap<>();
        Objects.requireNonNull(data).forEach((k,v) -> copy.put(Objects.requireNonNull(k),Objects.requireNonNull(v)));
        data = java.util.Collections.unmodifiableMap(copy);
        WordInlineObject.checkSize(width,height);
        if (width < 4 || height < 4) throw new IllegalArgumentException("Custom object is too small");
        altText = altText == null ? "" : altText;
        placement = placement == null ? WordPlacement.INLINE : placement;
    }
    public static WordCustomObject of(String customType, Map<String,String> data, float width, float height) {
        return new WordCustomObject(WordIds.next(),customType,data,width,height,"",WordPlacement.INLINE,null);
    }
    @Override public String type() { return customType; }
    @Override public WordCustomObject withId(String value) { return new WordCustomObject(value,customType,data,width,height,altText,placement,previewResourceId); }
    @Override public WordCustomObject resize(float w, float h) { return new WordCustomObject(id,customType,data,w,h,altText,placement,previewResourceId); }
    @Override public WordCustomObject withPlacement(WordPlacement value) { return new WordCustomObject(id,customType,data,width,height,altText,value,previewResourceId); }
    @Override public WordCustomObject withAltText(String value) { return new WordCustomObject(id,customType,data,width,height,value,placement,previewResourceId); }
    public WordCustomObject withData(Map<String,String> value) { return new WordCustomObject(id,customType,value,width,height,altText,placement,previewResourceId); }
    public WordCustomObject withPreview(String resourceId) { return new WordCustomObject(id,customType,data,width,height,altText,placement,resourceId); }
    @Override public String plainText() { return altText.isBlank() ? "[" + customType + "]" : "[" + altText + "]"; }
}
