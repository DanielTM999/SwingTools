package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.math.WordMathLayout;
import dtm.stools.component.panels.editor.word.model.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WordObjectRegistry {
    private final Map<String,WordObjectPainter> painters = new ConcurrentHashMap<>();

    public static WordObjectRegistry defaults() {
        WordObjectRegistry registry = new WordObjectRegistry();
        registry.painters.put(WordImage.TYPE,WordObjectRegistry::paintImage);
        registry.painters.put(WordChart.TYPE,new WordChartPainter());
        registry.painters.put(WordShape.TYPE,new WordShapePainter());
        registry.painters.put(WordDiagram.TYPE,new WordDiagramPainter());
        registry.painters.put(WordEquation.TYPE,WordObjectRegistry::paintEquation);
        registry.painters.put(WordOpaqueObject.TYPE,(g,o,b,d) -> WordPaintSupport.placeholder(g,b,"Conteúdo preservado: " + ((WordOpaqueObject)o).label()));
        return registry;
    }
    public ProviderRegistration register(String type, WordObjectPainter painter) {
        Objects.requireNonNull(type); Objects.requireNonNull(painter);
        if (painters.putIfAbsent(type,painter) != null) throw new IllegalArgumentException("Painter already registered: " + type);
        return () -> painters.remove(type,painter);
    }
    public Optional<WordObjectPainter> painter(String type) { return Optional.ofNullable(painters.get(type)); }
    public boolean supports(String type) { return painters.containsKey(type); }

    public void paint(Graphics2D graphics, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            WordPaintSupport.quality(g);
            if (object.rotation() != 0) g.rotate(Math.toRadians(object.rotation()),bounds.getCenterX(),bounds.getCenterY());
            WordObjectPainter painter = painters.get(object.type());
            if (painter != null) { painter.paint(g,object,bounds,document); return; }
            if (object instanceof WordCustomObject custom) {
                BufferedImage preview = custom.previewResourceId() == null || document == null ? null
                        : document.resources().get(custom.previewResourceId()).flatMap(WordImageCache::image).orElse(null);
                if (preview != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.drawImage(preview,Math.round(bounds.x),Math.round(bounds.y),Math.round(bounds.width),Math.round(bounds.height),null);
                    g.setColor(new Color(0x9CA3AF)); g.setStroke(new BasicStroke(0.6f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{2,2},0)); g.draw(bounds);
                } else WordPaintSupport.placeholder(g,bounds,"Objeto \"" + custom.customType() + "\" — provider indisponível; dados preservados");
                return;
            }
            WordPaintSupport.placeholder(g,bounds,object.type());
        } finally { g.dispose(); }
    }

    private static void paintImage(Graphics2D g, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        WordImage image = (WordImage)object;
        WordResource resource = document == null ? null : document.resources().get(image.resourceId()).orElse(null);
        BufferedImage decoded = WordImageCache.image(resource).orElse(null);
        if (decoded == null) { WordPaintSupport.placeholder(g,bounds,"Imagem" + (resource == null ? " ausente" : " (" + resource.contentType() + ")") + (image.altText().isBlank() ? "" : ": " + image.altText())); return; }
        WordCrop crop = image.crop(); int w = decoded.getWidth(), h = decoded.getHeight();
        int sx1 = Math.round(crop.left()*w), sy1 = Math.round(crop.top()*h), sx2 = Math.max(sx1+1,Math.round(w-crop.right()*w)), sy2 = Math.max(sy1+1,Math.round(h-crop.bottom()*h));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform();
        transform.translate(bounds.x,bounds.y); transform.scale(bounds.width/(double)(sx2-sx1),bounds.height/(double)(sy2-sy1)); transform.translate(-sx1,-sy1);
        Shape old = g.getClip(); g.clip(bounds);
        g.drawImage(decoded,transform,null);
        g.setClip(old);
    }

    private static void paintEquation(Graphics2D g, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        WordEquation equation = (WordEquation)object;
        WordMathLayout.Box box = WordMathLayout.measure(equation.math(),equation.fontSize());
        double sx = bounds.width/Math.max(1,box.width()+2), sy = bounds.height/Math.max(1,box.height()+2);
        Graphics2D c = (Graphics2D)g.create();
        try {
            c.translate(bounds.x,bounds.y); c.scale(sx,sy); c.setColor(new Color(0x111111));
            WordMathLayout.paint(c,equation.math(),equation.fontSize(),1,box.ascent()+1);
        } finally { c.dispose(); }
    }
}
