package dtm.stools.component.panels.skeleton;

import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder de carregamento com formas geométricas e brilho deslizante.
 */
public class SkeletonPanel extends PanelEventListener {

    /**
     * Forma de cada bloco do placeholder.
     */
    public enum Shape2D {
        TEXT, RECT, CIRCLE
    }

    /**
     * Bloco desenhado pelo placeholder.
     */
    public record Block(Shape2D shape, int height, double widthRatio) {
    }

    private final List<Block> blocks = new ArrayList<>();

    private boolean animated = true;
    private int animationPeriod = 1300;
    private float shimmerPosition;
    private long animationStartedAtNanos;

    private int blockGap = UiTokens.space(2);
    private int arc = UiTokens.radius(UiTokens.Radius.SM);

    private Color baseColor;
    private Color highlightColor;

    private final Timer animationTimer;

    public SkeletonPanel() {
        super(null, false);
        setOpaque(false);
        animationTimer = new Timer(32, e -> updateAnimation());
        addTextLines(3);
        updatePreferredSize();
    }

    /**
     * Adiciona um bloco ao placeholder.
     */
    public SkeletonPanel addBlock(Shape2D shape, int height, double widthRatio) {
        if (shape == null) {
            throw new IllegalArgumentException("shape cannot be null");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than zero");
        }
        blocks.add(new Block(shape, height, Math.max(0d, Math.min(1d, widthRatio))));
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Adiciona linhas de texto com larguras decrescentes.
     */
    public SkeletonPanel addTextLines(int lines) {
        if (lines <= 0) {
            throw new IllegalArgumentException("lines must be greater than zero");
        }
        for (int i = 0; i < lines; i++) {
            addBlock(Shape2D.TEXT, UiTokens.scale(12), i == lines - 1 ? 0.6d : 1d);
        }
        return this;
    }

    /**
     * Adiciona um avatar circular seguido de linhas de texto.
     */
    public SkeletonPanel addAvatarWithLines(int size, int lines) {
        addBlock(Shape2D.CIRCLE, size, 0d);
        return addTextLines(lines);
    }

    /**
     * Remove todos os blocos do placeholder.
     */
    public SkeletonPanel clearBlocks() {
        blocks.clear();
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita o brilho deslizante.
     */
    public SkeletonPanel setAnimated(boolean animated) {
        this.animated = animated;
        if (animated) {
            startAnimation();
        } else {
            animationTimer.stop();
            shimmerPosition = 0f;
            repaint();
        }
        return this;
    }

    /**
     * Define o período completo do brilho em milissegundos.
     */
    public SkeletonPanel setAnimationPeriod(int animationPeriod) {
        if (animationPeriod <= 0) {
            throw new IllegalArgumentException("animationPeriod must be greater than zero");
        }
        this.animationPeriod = animationPeriod;
        return this;
    }

    /**
     * Define o espaço vertical entre os blocos.
     */
    public SkeletonPanel setBlockGap(int blockGap) {
        if (blockGap < 0) {
            throw new IllegalArgumentException("blockGap cannot be negative");
        }
        this.blockGap = blockGap;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define as cores da base e do brilho.
     */
    public SkeletonPanel setColors(Color baseColor, Color highlightColor) {
        this.baseColor = baseColor;
        this.highlightColor = highlightColor;
        repaint();
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (animated) {
            startAnimation();
        }
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    private void startAnimation() {
        animationStartedAtNanos = System.nanoTime();
        animationTimer.start();
    }

    private void updateAnimation() {
        long elapsed = (System.nanoTime() - animationStartedAtNanos) / 1_000_000L;
        shimmerPosition = (float) (elapsed % animationPeriod) / animationPeriod;
        repaint();
    }

    private void updatePreferredSize() {
        if (blocks == null) {
            return;
        }
        int height = 0;
        for (Block block : blocks) {
            height += block.height() + blockGap;
        }
        height = Math.max(0, height - blockGap);
        setPreferredSize(new Dimension(UiTokens.scale(240), height));
        setMinimumSize(new Dimension(UiTokens.scale(80), height));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            int y = 0;
            for (Block block : blocks) {
                paintBlock(g2, block, y);
                y += block.height() + blockGap;
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta um bloco individual do placeholder.
     */
    protected void paintBlock(Graphics2D g2, Block block, int y) {
        Shape shape = buildShape(block, y);
        g2.setPaint(resolvePaint());
        g2.fill(shape);
    }

    private Shape buildShape(Block block, int y) {
        if (block.shape() == Shape2D.CIRCLE) {
            return new java.awt.geom.Ellipse2D.Float(0, y, block.height(), block.height());
        }
        int width = Math.max(1, (int) Math.round(getWidth() * block.widthRatio()));
        int radius = block.shape() == Shape2D.TEXT ? block.height() / 2 : arc;
        return PaintUtils.roundRect(0, y, width, block.height(), radius);
    }

    private java.awt.Paint resolvePaint() {
        Color base = baseColor != null ? baseColor : UiTokens.overlay(UiTokens.muted(), 0.18f);
        if (!animated) {
            return base;
        }

        Color highlight = highlightColor != null ? highlightColor : UiTokens.overlay(UiTokens.surface(), 0.75f);
        float span = Math.max(1f, getWidth());
        float center = (shimmerPosition * 2f - 0.5f) * span;
        return new GradientPaint(center - span * 0.2f, 0, base, center, 0, highlight, true);
    }

    /**
     * Blocos configurados no placeholder.
     */
    public List<Block> getBlocks() {
        return List.copyOf(blocks);
    }

    /**
     * Retângulo ocupado por um bloco na posição informada.
     */
    protected Rectangle getBlockBounds(int index) {
        int y = 0;
        for (int i = 0; i < index && i < blocks.size(); i++) {
            y += blocks.get(i).height() + blockGap;
        }
        Block block = blocks.get(index);
        int width = block.shape() == Shape2D.CIRCLE
                ? block.height()
                : (int) Math.round(getWidth() * block.widthRatio());
        return new Rectangle(0, y, width, block.height());
    }
}
