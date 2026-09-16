package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;

import dtm.stools.component.icon.FittedIcon;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JTextFieldListener extends JTextField implements EventListenerComponent {

    private final Set<String> validEvents = new HashSet<>();
    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    public JTextFieldListener(){
        init();
    }

    public JTextFieldListener(int columns){
        super(columns);
        init();
    }

    private static final int DEFAULT_CLEAR_SIZE = 16;
    private static final double CLEAR_GLYPH_DESIGN_SIZE = 16d;
    private static final double[] CLEAR_GLYPH_CROSS = {
            4.5, 5.5, 5.5, 4.5, 8, 7, 10.5, 4.5, 11.5, 5.5, 9, 8,
            11.5, 10.5, 10.5, 11.5, 8, 9, 5.5, 11.5, 4.5, 10.5, 7, 8
    };

    private Icon icon;
    private Color iconColor;
    private int iconGap = 6;
    private boolean clearButtonEnabled = true;
    private Icon clearIcon;
    private Color clearIconColor;

    private Rectangle clearBounds;
    private boolean clearHovered;
    private boolean clearPressed;

    private Icon tintCacheResult;
    private Icon tintCacheSource;
    private Color tintCacheColor;
    private int tintCacheSize;

    private final DocumentListener repaintOnTextChange = new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { repaint(); }
        @Override public void removeUpdate(DocumentEvent e) { repaint(); }
        @Override public void changedUpdate(DocumentEvent e) { repaint(); }
    };

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        clearTintCache();
        refreshLayout();
    }

    public Color getIconColor() {
        return iconColor;
    }

    public void setIconColor(Color iconColor) {
        this.iconColor = iconColor;
        clearTintCache();
        repaint();
    }

    public int getIconGap() {
        return iconGap;
    }

    public void setIconGap(int iconGap) {
        this.iconGap = Math.max(0, iconGap);
        refreshLayout();
    }

    public boolean isClearButtonEnabled() {
        return clearButtonEnabled;
    }

    public void setClearButtonEnabled(boolean clearButtonEnabled) {
        this.clearButtonEnabled = clearButtonEnabled;
        refreshLayout();
    }

    public Icon getClearIcon() {
        return clearIcon;
    }

    public void setClearIcon(Icon clearIcon) {
        this.clearIcon = clearIcon;
        refreshLayout();
    }

    public Color getClearIconColor() {
        return clearIconColor;
    }

    public void setClearIconColor(Color clearIconColor) {
        this.clearIconColor = clearIconColor;
        repaint();
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        return new Insets(insets.top, insets.left + leadingExtent(),
                insets.bottom, insets.right + trailingExtent());
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets resolved = super.getInsets(insets);
        resolved.left += leadingExtent();
        resolved.right += trailingExtent();
        return resolved;
    }

    @Override
    public void setDocument(Document document) {
        Document previous = getDocument();
        if (previous != null) {
            previous.removeDocumentListener(repaintOnTextChange);
        }
        super.setDocument(document);
        if (document != null && repaintOnTextChange != null) {
            document.addDocumentListener(repaintOnTextChange);
        }
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        refreshLayout();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        refreshLayout();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            paintLeadingIcon(g2);
            paintClearButton(g2);
        } finally {
            g2.dispose();
        }
    }

    protected void performClear() {
        if (getText().isEmpty()) return;
        setText("");
        dispachEvent(EventType.CLEAR, this, "");
        requestFocusInWindow();
    }

    protected boolean isClearButtonVisible() {
        return isClearButtonReserved() && !getText().isEmpty();
    }

    private boolean isClearButtonReserved() {
        return clearButtonEnabled && isEditable() && isEnabled();
    }

    private int clearSlotWidth() {
        return clearIcon != null ? Math.max(1, clearIcon.getIconWidth()) : DEFAULT_CLEAR_SIZE;
    }

    private int leadingExtent() {
        return icon == null ? 0 : Math.max(1, icon.getIconWidth()) + iconGap;
    }

    private int trailingExtent() {
        return isClearButtonReserved() ? clearSlotWidth() + iconGap : 0;
    }

    private void refreshLayout() {
        revalidate();
        repaint();
    }

    private void paintLeadingIcon(Graphics2D g2) {
        if (icon == null) return;
        Insets insets = super.getInsets();
        int available = getHeight() - insets.top - insets.bottom;
        if (available <= 0) return;
        Icon resolved = resolveIcon(available);
        if (resolved == null) return;
        int x = insets.left;
        int y = (getHeight() - resolved.getIconHeight()) / 2;
        resolved.paintIcon(this, g2, x, y);
    }

    private void paintClearButton(Graphics2D g2) {
        if (!isClearButtonVisible()) {
            clearBounds = null;
            return;
        }
        Insets insets = super.getInsets();
        int available = getHeight() - insets.top - insets.bottom;
        if (available <= 0) {
            clearBounds = null;
            return;
        }
        int size = clearIcon != null
                ? Math.max(1, clearIcon.getIconHeight())
                : Math.min(DEFAULT_CLEAR_SIZE, Math.max(8, available));
        int width = clearIcon != null ? Math.max(1, clearIcon.getIconWidth()) : size;
        int x = getWidth() - insets.right - width;
        int y = (getHeight() - size) / 2;
        clearBounds = new Rectangle(x - 3, y - 3, width + 6, size + 6);

        if (clearIcon != null) {
            clearIcon.paintIcon(this, g2, x, y);
            return;
        }

        paintClearGlyph(g2, x, y, size);
    }

    private void paintClearGlyph(Graphics2D g2, int x, int y, int size) {
        double scale = size / CLEAR_GLYPH_DESIGN_SIZE;
        Graphics2D glyph = (Graphics2D) g2.create();
        try {
            glyph.translate(x, y);
            glyph.scale(scale, scale);
            if (clearHovered || clearPressed) {
                glyph.setColor(clearPressed ? resolveClearPressedColor() : resolveClearHoverColor());
                Path2D.Float filled = new Path2D.Float(Path2D.WIND_EVEN_ODD);
                filled.append(new Ellipse2D.Float(1.75f, 1.75f, 12.5f, 12.5f), false);
                filled.append(clearCrossPath(), false);
                glyph.fill(filled);
            } else {
                glyph.setColor(resolveClearColor());
                glyph.setStroke(new BasicStroke((float) (1d / scale)));
                Path2D.Float cross = new Path2D.Float(Path2D.WIND_NON_ZERO, 4);
                cross.moveTo(5, 5);
                cross.lineTo(11, 11);
                cross.moveTo(5, 11);
                cross.lineTo(11, 5);
                glyph.draw(cross);
            }
        } finally {
            glyph.dispose();
        }
    }

    private static Path2D.Float clearCrossPath() {
        Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, CLEAR_GLYPH_CROSS.length / 2);
        path.moveTo(CLEAR_GLYPH_CROSS[0], CLEAR_GLYPH_CROSS[1]);
        for (int i = 2; i < CLEAR_GLYPH_CROSS.length; i += 2) {
            path.lineTo(CLEAR_GLYPH_CROSS[i], CLEAR_GLYPH_CROSS[i + 1]);
        }
        path.closePath();
        return path;
    }

    private Color resolveClearColor() {
        if (clearIconColor != null) {
            return clearIconColor;
        }
        Color flat = UIManager.getColor("SearchField.clearIconColor");
        if (flat != null) {
            return flat;
        }
        Color disabled = UIManager.getColor("Label.disabledForeground");
        return disabled != null ? disabled : ColorUtils.withAlpha(getForeground(), 140);
    }

    private Color resolveClearHoverColor() {
        if (clearIconColor != null) {
            return clearIconColor;
        }
        Color flat = UIManager.getColor("SearchField.clearIconHoverColor");
        return flat != null ? flat : resolveClearColor();
    }

    private Color resolveClearPressedColor() {
        if (clearIconColor == null) {
            Color flat = UIManager.getColor("SearchField.clearIconPressedColor");
            if (flat != null) {
                return flat;
            }
        }
        return ColorUtils.withAlpha(resolveClearColor(), 204);
    }

    private Icon resolveIcon(int maxSize) {
        Icon fitted = FittedIcon.fit(icon, maxSize);
        if (iconColor == null) return fitted;
        if (tintCacheResult != null
                && tintCacheSource == icon
                && tintCacheSize == maxSize
                && iconColor.equals(tintCacheColor)) {
            return tintCacheResult;
        }
        Icon tinted = tint(fitted, iconColor);
        tintCacheResult = tinted;
        tintCacheSource = icon;
        tintCacheSize = maxSize;
        tintCacheColor = iconColor;
        return tinted;
    }

    private Icon tint(Icon source, Color color) {
        if (source == null) return null;
        int width = Math.max(1, source.getIconWidth());
        int height = Math.max(1, source.getIconHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            source.paintIcon(this, g2, 0, 0);
            g2.setComposite(AlphaComposite.SrcAtop);
            g2.setColor(color);
            g2.fillRect(0, 0, width, height);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(image);
    }

    private void clearTintCache() {
        tintCacheResult = null;
        tintCacheSource = null;
        tintCacheColor = null;
        tintCacheSize = 0;
    }

    private boolean isOverClearButton(Point point) {
        return isClearButtonVisible() && clearBounds != null && clearBounds.contains(point);
    }

    private void installClearButtonHandler() {
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(isOverClearButton(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHover(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (!isOverClearButton(e.getPoint())) return;
                e.consume();
                clearPressed = true;
                performClear();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!clearPressed) return;
                clearPressed = false;
                repaint();
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private void updateHover(boolean hovered) {
        if (clearHovered == hovered) return;
        clearHovered = hovered;
        setCursor(Cursor.getPredefinedCursor(hovered ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
        repaint();
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return () -> {};

        if(!validEvents.contains(eventType)) return () -> {};

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        listeners.get(eventType).remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            dispachEvent(EventType.LOAD, this, this);
        });
    }

    protected void registerValidEvents(Set<String> events){
        events.add(EventType.LOAD);
        events.add(EventType.CHANGE);
        events.add(EventType.INPUT);
        events.add(EventType.RESIZE);
        events.add(EventType.CLEAR);
        events.add(EventType.SUBMIT);
    }

    protected void dispachEvent(String eventType, Object value){
        dispachEvent(eventType, JTextFieldListener.this, value);
    }

    protected <T> void dispachEvent(String eventType, Supplier<T> value){
        dispachEvent(eventType, JTextFieldListener.this, value);
    }

    protected void dispachEvent(String eventType, Component component, Object value){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T tryGetValue() {
                        try {
                            return (T) value;
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    protected <T> void dispachEvent(String eventType, Component component, Supplier<T> value){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value.get();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public T tryGetValue() {
                        try {
                            return value.get();
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void init(){
        registerValidEvents(validEvents);
        Document document = getDocument();
        if (document != null) {
            document.removeDocumentListener(repaintOnTextChange);
            document.addDocumentListener(repaintOnTextChange);
        }
        installClearButtonHandler();
    }

}
