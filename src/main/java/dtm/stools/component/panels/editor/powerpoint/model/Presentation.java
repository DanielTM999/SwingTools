package dtm.stools.component.panels.editor.powerpoint.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable presentation. Coordinates use a logical 1280 by 720 canvas by default. */
public record Presentation(int width, int height, List<PptSlide> slides) {
    public Presentation {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid slide size");
        slides = List.copyOf(Objects.requireNonNull(slides));
        if (slides.isEmpty()) throw new IllegalArgumentException("A presentation needs a slide");
    }

    public static Presentation create() { return new Presentation(1280, 720, List.of(PptSlide.create("Slide 1"))); }
    public Presentation withSlide(int index, PptSlide slide) {
        List<PptSlide> copy = new ArrayList<>(slides);
        copy.set(index, Objects.requireNonNull(slide));
        return new Presentation(width, height, copy);
    }
    public Presentation insertSlide(int index, PptSlide slide) {
        List<PptSlide> copy = new ArrayList<>(slides);
        copy.add(index, Objects.requireNonNull(slide));
        return new Presentation(width, height, copy);
    }
    public Presentation removeSlide(int index) {
        if (slides.size() == 1) throw new IllegalStateException("The last slide cannot be deleted");
        List<PptSlide> copy = new ArrayList<>(slides);
        copy.remove(index);
        return new Presentation(width, height, copy);
    }
    public Presentation moveSlide(int from, int to) {
        List<PptSlide> copy = new ArrayList<>(slides);
        copy.add(to, copy.remove(from));
        return new Presentation(width, height, copy);
    }
}
