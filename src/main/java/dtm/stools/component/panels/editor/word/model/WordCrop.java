package dtm.stools.component.panels.editor.word.model;

public record WordCrop(float left, float top, float right, float bottom) {
    public static final WordCrop NONE = new WordCrop(0,0,0,0);
    public WordCrop {
        for (float v : new float[]{left,top,right,bottom}) if (!Float.isFinite(v) || v < 0 || v > 0.95f) throw new IllegalArgumentException("Invalid crop");
        if (left+right > 0.95f || top+bottom > 0.95f) throw new IllegalArgumentException("Crop removes the whole image");
    }
    public boolean isEmpty() { return left == 0 && top == 0 && right == 0 && bottom == 0; }
}
