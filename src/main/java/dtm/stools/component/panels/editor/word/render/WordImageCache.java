package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.model.WordResource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WordImageCache {
    private static final Map<String,SoftReference<BufferedImage>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String,Boolean> FAILED = new ConcurrentHashMap<>();
    private WordImageCache() {}

    public static Optional<BufferedImage> image(WordResource resource) {
        if (resource == null || FAILED.containsKey(resource.id())) return Optional.empty();
        SoftReference<BufferedImage> ref = CACHE.get(resource.id());
        BufferedImage image = ref == null ? null : ref.get();
        if (image != null) return Optional.of(image);
        try {
            image = ImageIO.read(new ByteArrayInputStream(resource.data()));
        } catch (IOException | RuntimeException e) { image = null; }
        if (image == null) { FAILED.put(resource.id(),true); return Optional.empty(); }
        if ((long)image.getWidth()*image.getHeight() > 60_000_000L) { FAILED.put(resource.id(),true); return Optional.empty(); }
        CACHE.put(resource.id(),new SoftReference<>(image));
        return Optional.of(image);
    }
    public static Optional<java.awt.Dimension> size(WordResource resource) { return image(resource).map(i -> new java.awt.Dimension(i.getWidth(),i.getHeight())); }
}
