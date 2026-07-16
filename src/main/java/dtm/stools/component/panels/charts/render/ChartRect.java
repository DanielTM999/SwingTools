package dtm.stools.component.panels.charts.render;

public final class ChartRect {

    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public ChartRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0f, width);
        this.height = Math.max(0f, height);
    }

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public float centerX() {
        return x + width * 0.5f;
    }

    public float centerY() {
        return y + height * 0.5f;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    public ChartRect inset(float left, float top, float right, float bottom) {
        return new ChartRect(x + left, y + top, width - left - right, height - top - bottom);
    }

    @Override
    public String toString() {
        return "ChartRect(" + x + ", " + y + ", " + width + "x" + height + ")";
    }
}
