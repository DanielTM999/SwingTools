package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfEraserMode;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.element.PdfElementFactory;
import dtm.stools.component.panels.editor.pdf.element.PdfPlacementMode;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PdfSelectionController extends MouseAdapter implements PdfCanvasOverlay {
    private static final int THRESHOLD = 3;
    private static final Color GHOST_OUTLINE = new Color(0x1F6FD1);

    private final PdfEditor editor;
    private final PdfCanvas canvas;
    private PdfDragMode drag = PdfDragMode.NONE;
    private boolean started;
    private int page = -1;
    private Point press, current;
    private PdfHandle handle;
    private Rectangle2D.Double startView;
    private BufferedImage ghost;
    private PdfElementFactory factory;
    private final List<Point2D.Double> stroke = new ArrayList<>();
    private PdfPageElement hover;
    private int hoverPage = -1;
    private boolean pendingSingle;
    private PdfPageElement pressedElement;
    private Point brush;
    private boolean brushShown;

    public PdfSelectionController(PdfEditor editor, PdfCanvas canvas) {
        this.editor = editor;
        this.canvas = canvas;
    }

    public void install() {
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addOverlay(this);
        InputMap keys = canvas.getInputMap(JComponent.WHEN_FOCUSED);
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "pdf.eraseSelection");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "pdf.eraseSelection");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "pdf.copy");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "pdf.cut");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "pdf.paste");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "pdf.duplicate");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "pdf.selectAll");
        bind(keys, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "pdf.editText");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pdf.canvas.escape");
        canvas.getActionMap().put("pdf.canvas.escape", action(this::escape));
        nudge(keys, KeyEvent.VK_LEFT, -1, 0);
        nudge(keys, KeyEvent.VK_RIGHT, 1, 0);
        nudge(keys, KeyEvent.VK_UP, 0, -1);
        nudge(keys, KeyEvent.VK_DOWN, 0, 1);
    }

    private void bind(InputMap keys, KeyStroke key, String command) {
        keys.put(key, command);
        canvas.getActionMap().put(command, action(() -> editor.execute(command)));
    }

    private void nudge(InputMap keys, int code, int dx, int dy) {
        keys.put(KeyStroke.getKeyStroke(code, 0), "pdf.nudge." + code);
        keys.put(KeyStroke.getKeyStroke(code, InputEvent.SHIFT_DOWN_MASK), "pdf.nudgeFar." + code);
        canvas.getActionMap().put("pdf.nudge." + code, action(() -> editor.nudgeSelection(dx, dy)));
        canvas.getActionMap().put("pdf.nudgeFar." + code, action(() -> editor.nudgeSelection(dx * 10, dy * 10)));
    }

    private static Action action(Runnable body) {
        return new AbstractAction() { @Override public void actionPerformed(ActionEvent event) { body.run(); } };
    }

    public void escape() {
        if (drag != PdfDragMode.NONE) { reset(); canvas.repaint(); return; }
        if (!editor.getSelection().isEmpty()) { editor.clearSelection(); return; }
        editor.selectTool(PdfEditor.TOOL_SELECT);
    }

    private PdfPageLayout layout() { return canvas.getPageLayout(); }

    @Override public void mousePressed(MouseEvent event) {
        canvas.requestFocusInWindow();
        editor.commitTextInput();
        press = event.getPoint();
        current = press;
        started = false;
        pendingSingle = false;
        pressedElement = null;
        PdfPageLayout layout = layout();
        int at = layout.pageAt(press);
        if (SwingUtilities.isRightMouseButton(event)) { contextMenu(event, at); return; }
        if (!SwingUtilities.isLeftMouseButton(event)) return;
        String tool = editor.getActiveTool();
        factory = editor.getActiveFactory();
        if (factory != null) {
            if (at < 0) return;
            page = at;
            drag = factory.placementMode() == PdfPlacementMode.FREEHAND ? PdfDragMode.FREEHAND : PdfDragMode.PLACE;
            stroke.clear();
            stroke.add(new Point2D.Double(press.x, press.y));
            return;
        }
        if (PdfEditor.TOOL_ERASER.equals(tool)) {
            page = at >= 0 ? at : layout.nearestPage(press);
            drag = page >= 0 ? PdfDragMode.ERASE : PdfDragMode.NONE;
            stroke.clear();
            stroke.add(new Point2D.Double(press.x, press.y));
            if (editor.getEraserMode() == PdfEraserMode.BRUSH) started = true;
            canvas.repaint(canvas.getVisibleRect());
            return;
        }
        PdfSelection selection = editor.getSelection();
        Rectangle2D.Double selected = selectionView();
        if (selected != null && !event.isShiftDown()) {
            PdfHandle grabbed = handleAt(selected, press);
            if (grabbed != null) {
                begin(grabbed == PdfHandle.ROTATE ? PdfDragMode.ROTATE : PdfDragMode.RESIZE, selection.page(), grabbed, selected);
                return;
            }
        }
        if (event.getClickCount() == 2 && at >= 0 && PdfEditor.TOOL_SELECT.equals(tool)) {
            PdfPageElement hit = editor.elementAt(at, layout.toPdf(at, press), tolerance(at));
            if (hit != null) { editor.editElement(at, hit); return; }
        }
        if (selected != null && grow(selected, 2).contains(press) && !event.isShiftDown()) {
            if (PdfEditor.TOOL_SELECT.equals(tool) && at == selection.page()) {
                PdfPageElement hit = editor.elementAt(at, layout.toPdf(at, press), tolerance(at));
                if (hit != null && selection.elements().size() > 1 && selection.contains(hit.id())) { pendingSingle = true; pressedElement = hit; }
            }
            begin(PdfDragMode.MOVE, selection.page(), null, selected);
            return;
        }
        if (PdfEditor.TOOL_SELECT.equals(tool) && at >= 0) {
            PdfPageElement hit = editor.elementAt(at, layout.toPdf(at, press), tolerance(at));
            if (hit != null) {
                if (event.isShiftDown() && selection.page() == at && !selection.hasArea()) {
                    List<PdfPageElement> elements = new ArrayList<>(selection.elements());
                    if (!elements.removeIf(element -> element.id().equals(hit.id()))) elements.add(hit);
                    editor.setSelection(PdfSelection.of(at, elements));
                    return;
                }
                editor.setSelection(PdfSelection.of(at, List.of(hit)));
                begin(PdfDragMode.MOVE, at, null, selectionView());
                return;
            }
        }
        if (!event.isShiftDown()) editor.clearSelection();
        page = at >= 0 ? at : layout.nearestPage(press);
        drag = page >= 0 ? PdfDragMode.MARQUEE : PdfDragMode.NONE;
    }

    private void begin(PdfDragMode mode, int selectionPage, PdfHandle grabbed, Rectangle2D.Double view) {
        if (view == null) return;
        drag = mode;
        page = selectionPage;
        handle = grabbed;
        startView = view;
        ghost = capture(selectionPage, view);
    }

    private BufferedImage capture(int target, Rectangle2D view) {
        Rectangle box = layout().bounds(target);
        if (box == null) return null;
        BufferedImage image = editor.getRenderer().page(target, canvas.renderResolution(), null);
        if (image == null) return null;
        double sx = image.getWidth() / (double) box.width, sy = image.getHeight() / (double) box.height;
        int x = (int) Math.floor((view.getX() - box.x) * sx), y = (int) Math.floor((view.getY() - box.y) * sy);
        int w = (int) Math.ceil(view.getWidth() * sx), h = (int) Math.ceil(view.getHeight() * sy);
        x = Math.max(0, x); y = Math.max(0, y);
        w = Math.min(w, image.getWidth() - x); h = Math.min(h, image.getHeight() - y);
        if (w <= 0 || h <= 0) return null;
        return image.getSubimage(x, y, w, h);
    }

    @Override public void mouseDragged(MouseEvent event) {
        if (drag == PdfDragMode.NONE || press == null) return;
        current = event.getPoint();
        if (!started && press.distance(current) >= THRESHOLD) started = true;
        if (drag == PdfDragMode.FREEHAND || drag == PdfDragMode.ERASE) stroke.add(new Point2D.Double(current.x, current.y));
        brush = current;
        canvas.scrollRectToVisible(new Rectangle(current.x - 8, current.y - 8, 16, 16));
        canvas.repaint(canvas.getVisibleRect());
    }

    @Override public void mouseReleased(MouseEvent event) {
        if (drag == PdfDragMode.NONE || press == null || !SwingUtilities.isLeftMouseButton(event)) { reset(); return; }
        current = event.getPoint();
        PdfDragMode mode = drag;
        int target = page;
        boolean moved = started;
        PdfPageLayout layout = layout();
        try {
            if (!layout.contains(target)) return;
            switch (mode) {
                case PLACE -> place(target, moved);
                case FREEHAND -> freehand(target);
                case ERASE -> {
                    if (editor.getEraserMode() == PdfEraserMode.BRUSH) editor.eraseShape(target, brushShape(target));
                    else {
                        Rectangle2D.Float area = layout.toPdf(target, clip(target, marquee()));
                        if (moved && area.width > 1 && area.height > 1) editor.eraseRegion(target, area);
                    }
                }
                case MARQUEE -> { if (moved) marqueeSelect(target, event.isShiftDown()); }
                case MOVE -> {
                    if (!moved) {
                        if (pendingSingle && pressedElement != null) editor.setSelection(PdfSelection.of(target, List.of(pressedElement)));
                        return;
                    }
                    Point2D.Float a = layout.toPdf(target, press), b = layout.toPdf(target, current);
                    editor.transformSelection(AffineTransform.getTranslateInstance(b.x - a.x, b.y - a.y), "Mover");
                }
                case RESIZE -> {
                    if (!moved) return;
                    Rectangle2D.Float from = layout.toPdf(target, startView), to = layout.toPdf(target, resized(event.isShiftDown()));
                    if (from.width <= 0 || from.height <= 0) return;
                    AffineTransform transform = new AffineTransform();
                    transform.translate(to.x, to.y);
                    transform.scale(to.width / from.width, to.height / from.height);
                    transform.translate(-from.x, -from.y);
                    editor.transformSelection(transform, "Redimensionar");
                }
                case ROTATE -> {
                    if (!moved) return;
                    double degrees = angle(event.isShiftDown());
                    Point2D.Float center = layout.toPdf(target, new Point2D.Double(startView.getCenterX(), startView.getCenterY()));
                    editor.transformSelection(AffineTransform.getRotateInstance(Math.toRadians(-degrees), center.x, center.y), "Girar");
                }
                default -> { }
            }
        } finally {
            reset();
            canvas.repaint();
        }
    }

    private void place(int target, boolean moved) {
        PdfPageLayout layout = layout();
        Point2D.Float start = layout.toPdfClamped(target, press);
        Rectangle2D.Float bounds;
        if (moved) {
            Point2D.Float end = layout.toPdfClamped(target, current);
            bounds = new Rectangle2D.Float(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.abs(end.x - start.x), Math.abs(end.y - start.y));
            if (factory.placementMode() == PdfPlacementMode.CLICK) bounds = new Rectangle2D.Float(start.x, start.y, 0, 0);
        } else bounds = new Rectangle2D.Float(start.x, start.y, 0, 0);
        editor.placeElement(factory, new PdfPlacement(target, start, bounds));
    }

    private void freehand(int target) {
        if (stroke.size() < 2) return;
        PdfPageLayout layout = layout();
        float[] points = new float[stroke.size() * 2];
        for (int i = 0; i < stroke.size(); i++) {
            Point2D.Float point = layout.toPdfClamped(target, stroke.get(i));
            points[2 * i] = point.x;
            points[2 * i + 1] = point.y;
        }
        Point2D.Float start = new Point2D.Float(points[0], points[1]);
        editor.placeElement(factory, new PdfPlacement(target, start, null, points));
    }

    private Shape brushShape(int target) {
        PdfPageLayout layout = layout();
        float size = editor.getEraserSize();
        Point2D.Float first = layout.toPdf(target, stroke.getFirst());
        if (stroke.size() < 2) return new Ellipse2D.Float(first.x - size / 2, first.y - size / 2, size, size);
        Path2D.Float path = new Path2D.Float();
        path.moveTo(first.x, first.y);
        for (int i = 1; i < stroke.size(); i++) {
            Point2D.Float point = layout.toPdf(target, stroke.get(i));
            path.lineTo(point.x, point.y);
        }
        return new Area(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(path));
    }

    private void marqueeSelect(int target, boolean add) {
        PdfPageLayout layout = layout();
        Rectangle2D.Float area = layout.toPdf(target, clip(target, marquee()));
        if (area.width < 1 || area.height < 1) return;
        List<PdfPageElement> inside = new ArrayList<>();
        for (PdfPageElement element : editor.getPageElements(target)) {
            Rectangle2D.Float bounds = element.bounds();
            boolean contained = element.textual() ? area.contains(bounds.getCenterX(), bounds.getCenterY())
                    : area.contains(bounds.x + .5, bounds.y + .5, Math.max(0, bounds.width - 1), Math.max(0, bounds.height - 1));
            if (contained) inside.add(element);
        }
        if (PdfEditor.TOOL_AREA.equals(editor.getActiveTool())) {
            editor.setSelection(PdfSelection.area(target, inside, area));
            return;
        }
        PdfSelection selection = editor.getSelection();
        if (add && selection.page() == target && !selection.hasArea()) {
            List<PdfPageElement> merged = new ArrayList<>(selection.elements());
            for (PdfPageElement element : inside) if (!selection.contains(element.id())) merged.add(element);
            inside = merged;
        }
        editor.setSelection(inside.isEmpty() ? PdfSelection.area(target, List.of(), area) : PdfSelection.of(target, inside));
    }

    private void reset() {
        drag = PdfDragMode.NONE;
        started = false;
        handle = null;
        startView = null;
        ghost = null;
        stroke.clear();
        pendingSingle = false;
        pressedElement = null;
    }

    @Override public void mouseMoved(MouseEvent event) {
        Point point = event.getPoint();
        PdfPageLayout layout = layout();
        int at = layout.pageAt(point);
        if (editor.getActiveFactory() != null) { canvas.setCursor(Cursor.getPredefinedCursor(at >= 0 ? editor.getActiveFactory().cursor() : Cursor.DEFAULT_CURSOR)); setHover(null, -1); return; }
        String tool = editor.getActiveTool();
        brush = PdfEditor.TOOL_ERASER.equals(tool) && editor.getEraserMode() == PdfEraserMode.BRUSH && at >= 0 ? point : null;
        if (brush != null || brushShown) { brushShown = brush != null; canvas.repaint(canvas.getVisibleRect()); }
        if (PdfEditor.TOOL_ERASER.equals(tool) || PdfEditor.TOOL_AREA.equals(tool)) {
            Rectangle2D.Double selected = selectionView();
            Cursor cursor = Cursor.getPredefinedCursor(at >= 0 ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR);
            if (PdfEditor.TOOL_AREA.equals(tool) && selected != null) {
                PdfHandle over = handleAt(selected, point);
                if (over != null) cursor = Cursor.getPredefinedCursor(over.cursor());
                else if (selected.contains(point)) cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            }
            canvas.setCursor(cursor);
            setHover(null, -1);
            return;
        }
        Rectangle2D.Double selected = selectionView();
        if (selected != null) {
            PdfHandle over = handleAt(selected, point);
            if (over != null) { canvas.setCursor(Cursor.getPredefinedCursor(over.cursor())); setHover(null, -1); return; }
        }
        PdfPageElement hit = at >= 0 ? editor.cachedElementAt(at, layout.toPdf(at, point), tolerance(at)) : null;
        setHover(hit, at);
        boolean inside = selected != null && selected.contains(point);
        canvas.setCursor(Cursor.getPredefinedCursor(inside || hit != null ? (hit != null && hit.textBox() && !inside ? Cursor.TEXT_CURSOR : Cursor.MOVE_CURSOR) : Cursor.DEFAULT_CURSOR));
    }

    @Override public void mouseExited(MouseEvent event) {
        setHover(null, -1);
        if (brush != null) { brush = null; brushShown = false; canvas.repaint(canvas.getVisibleRect()); }
    }

    private void setHover(PdfPageElement element, int target) {
        if (element == hover && target == hoverPage) return;
        if (element != null && hover != null && element.id().equals(hover.id()) && target == hoverPage) return;
        hover = element;
        hoverPage = target;
        canvas.repaint(canvas.getVisibleRect());
    }

    @Override public void mouseWheelMoved(MouseWheelEvent event) {
        if (event.isControlDown()) {
            editor.zoomAt(event.getWheelRotation() < 0 ? 1.1 : 1 / 1.1, event.getPoint());
            return;
        }
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, canvas);
        if (scroll != null) scroll.dispatchEvent(SwingUtilities.convertMouseEvent(canvas, event, scroll));
    }

    private void contextMenu(MouseEvent event, int at) {
        if (at >= 0) {
            PdfSelection selection = editor.getSelection();
            PdfPageElement hit = editor.elementAt(at, layout().toPdf(at, event.getPoint()), tolerance(at));
            Rectangle2D.Double selected = selectionView();
            boolean inside = selected != null && selected.contains(event.getPoint());
            if (hit != null && !selection.contains(hit.id()) && !inside) editor.setSelection(PdfSelection.of(at, List.of(hit)));
            editor.setPastePoint(at, layout().toPdf(at, event.getPoint()));
        }
        JPopupMenu menu = new JPopupMenu();
        for (Action action : editor.canvasActions(at)) {
            if (action == null) { menu.addSeparator(); continue; }
            javax.swing.JMenuItem item = Boolean.TRUE.equals(action.getValue(PdfRibbon.TOGGLE))
                    ? new javax.swing.JCheckBoxMenuItem(action) : new javax.swing.JMenuItem(action);
            if (!PdfRibbon.iconOf(action).isEmpty()) item.setIcon(PdfIcon.small(PdfRibbon.iconOf(action)));
            menu.add(item);
        }
        if (menu.getComponentCount() > 0) menu.show(canvas, event.getX(), event.getY());
    }

    private double tolerance(int target) {
        PdfPageGeometry geometry = layout().geometry(target);
        return geometry == null ? 3 : 4 / geometry.scale();
    }

    public Rectangle2D.Double selectionView() {
        PdfSelection selection = editor.getSelection();
        if (selection.isEmpty() || !layout().contains(selection.page())) return null;
        Rectangle2D.Float bounds = selection.bounds();
        return bounds == null ? null : layout().toView(selection.page(), bounds);
    }

    private PdfHandle handleAt(Rectangle2D.Double selected, Point point) {
        for (PdfHandle candidate : PdfHandle.values()) if (grow(candidate.box(selected), 3).contains(point)) return candidate;
        return null;
    }

    private static Rectangle2D.Double grow(Rectangle2D rectangle, double amount) {
        return new Rectangle2D.Double(rectangle.getX() - amount, rectangle.getY() - amount,
                rectangle.getWidth() + 2 * amount, rectangle.getHeight() + 2 * amount);
    }

    private Rectangle2D.Double marquee() {
        return new Rectangle2D.Double(Math.min(press.x, current.x), Math.min(press.y, current.y),
                Math.abs(current.x - press.x), Math.abs(current.y - press.y));
    }

    private Rectangle2D clip(int target, Rectangle2D view) {
        Rectangle box = layout().bounds(target);
        Rectangle2D result = new Rectangle2D.Double();
        Rectangle2D.intersect(view, box, result);
        return result.getWidth() < 0 || result.getHeight() < 0 ? new Rectangle2D.Double(view.getX(), view.getY(), 0, 0) : result;
    }

    private Rectangle2D.Double resized(boolean keepAspect) {
        return handle.resize(startView, current.x - press.x, current.y - press.y, keepAspect);
    }

    private double angle(boolean snap) {
        double cx = startView.getCenterX(), cy = startView.getCenterY();
        double from = Math.atan2(press.y - cy, press.x - cx), to = Math.atan2(current.y - cy, current.x - cx);
        double degrees = Math.toDegrees(to - from);
        if (snap) degrees = Math.round(degrees / 15) * 15.0;
        return degrees;
    }

    @Override public void paintOverlay(Graphics2D g, PdfCanvas target) {
        PdfPageLayout layout = layout();
        PdfSelection selection = editor.getSelection();
        Color accent = UiTokens.accent();
        if (hover != null && layout.contains(hoverPage) && !selection.contains(hover.id()) && drag == PdfDragMode.NONE) {
            g.setColor(UiTokens.overlay(accent, .7f));
            g.setStroke(new BasicStroke(1f));
            g.draw(grow(layout.toView(hoverPage, hover.bounds()), 1.5));
        }
        boolean moving = started && (drag == PdfDragMode.MOVE || drag == PdfDragMode.RESIZE || drag == PdfDragMode.ROTATE);
        Rectangle2D.Double selected = selectionView();
        if (selected != null) {
            if (selection.hasArea()) {
                g.setColor(UiTokens.overlay(accent, .10f));
                g.fill(layout.toView(selection.page(), selection.area()));
            }
            g.setColor(UiTokens.overlay(accent, .85f));
            g.setStroke(new BasicStroke(1f));
            for (PdfPageElement element : selection.elements())
                if (selection.elements().size() > 1 || selection.hasArea()) g.draw(grow(layout.toView(selection.page(), element.bounds()), 1));
            if (!moving) paintFrame(g, selected, accent, true);
        }
        if (brush != null && PdfEditor.TOOL_ERASER.equals(editor.getActiveTool()) && editor.getEraserMode() == PdfEraserMode.BRUSH) {
            PdfPageGeometry geometry = layout.geometry(Math.max(0, layout.pageAt(brush)));
            double diameter = editor.getEraserSize() * (geometry == null ? 1 : geometry.scale());
            Ellipse2D.Double circle = new Ellipse2D.Double(brush.x - diameter / 2, brush.y - diameter / 2, diameter, diameter);
            g.setColor(UiTokens.overlay(UiTokens.danger(), .18f));
            g.fill(circle);
            g.setColor(UiTokens.danger());
            g.setStroke(new BasicStroke(1f));
            g.draw(circle);
        }
        if (drag == PdfDragMode.ERASE && editor.getEraserMode() == PdfEraserMode.BRUSH) {
            PdfPageGeometry geometry = layout.geometry(page);
            float width = (float) (editor.getEraserSize() * (geometry == null ? 1 : geometry.scale()));
            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i < stroke.size(); i++) {
                Point2D.Double point = stroke.get(i);
                if (i == 0) path.moveTo(point.x, point.y); else path.lineTo(point.x, point.y);
            }
            if (stroke.size() == 1) path.lineTo(stroke.getFirst().x + .01, stroke.getFirst().y);
            g.setColor(UiTokens.overlay(UiTokens.danger(), .35f));
            g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            return;
        }
        if (!started) return;
        switch (drag) {
            case MARQUEE, ERASE -> {
                Rectangle2D.Double box = marquee();
                Color color = drag == PdfDragMode.ERASE ? UiTokens.danger() : accent;
                g.setColor(UiTokens.overlay(color, drag == PdfDragMode.ERASE ? .22f : .12f));
                g.fill(box);
                g.setColor(color);
                g.setStroke(dashed(1.2f));
                g.draw(box);
            }
            case PLACE -> {
                g.setColor(accent);
                g.setStroke(dashed(1.4f));
                String icon = factory == null ? "" : factory.icon();
                if (icon.equals("line") || icon.equals("arrow")) g.draw(new Line2D.Double(press, current));
                else if (icon.equals("ellipse")) g.draw(new Ellipse2D.Double(marquee().x, marquee().y, marquee().width, marquee().height));
                else g.draw(marquee());
            }
            case FREEHAND -> {
                Path2D.Double path = new Path2D.Double();
                for (int i = 0; i < stroke.size(); i++) {
                    Point2D.Double point = stroke.get(i);
                    if (i == 0) path.moveTo(point.x, point.y); else path.lineTo(point.x, point.y);
                }
                PdfPageGeometry geometry = layout.geometry(page);
                float width = (float) (editor.getShapeStyle().lineWidth() * (geometry == null ? 1 : geometry.scale()));
                g.setColor(editor.getShapeStyle().stroke());
                g.setStroke(new BasicStroke(Math.max(1, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(path);
            }
            case MOVE -> paintGhost(g, AffineTransform.getTranslateInstance(current.x - press.x, current.y - press.y), startView);
            case RESIZE -> {
                Rectangle2D.Double to = resized(false);
                AffineTransform transform = new AffineTransform();
                transform.translate(to.x, to.y);
                transform.scale(to.width / startView.width, to.height / startView.height);
                transform.translate(-startView.x, -startView.y);
                paintGhost(g, transform, startView);
            }
            case ROTATE -> paintGhost(g, AffineTransform.getRotateInstance(Math.toRadians(angle(false)), startView.getCenterX(), startView.getCenterY()), startView);
            default -> { }
        }
    }

    private void paintGhost(Graphics2D g, AffineTransform transform, Rectangle2D.Double view) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.transform(transform);
            if (ghost != null) {
                copy.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, .78f));
                copy.drawImage(ghost, (int) Math.round(view.x), (int) Math.round(view.y), (int) Math.round(view.width), (int) Math.round(view.height), null);
                copy.setComposite(java.awt.AlphaComposite.SrcOver);
            }
            copy.setColor(GHOST_OUTLINE);
            copy.setStroke(dashed(1.2f));
            copy.draw(view);
        } finally { copy.dispose(); }
    }

    private void paintFrame(Graphics2D g, Rectangle2D.Double frame, Color accent, boolean handles) {
        g.setColor(accent);
        g.setStroke(dashed(1.2f));
        g.draw(frame);
        if (!handles) return;
        g.setStroke(new BasicStroke(1f));
        Point2D.Double top = PdfHandle.NORTH.point(frame), knob = PdfHandle.ROTATE.point(frame);
        g.draw(new Line2D.Double(top, knob));
        for (PdfHandle candidate : PdfHandle.values()) {
            Rectangle2D.Double box = candidate.box(frame);
            if (candidate == PdfHandle.ROTATE) {
                Ellipse2D.Double circle = new Ellipse2D.Double(box.x - 1, box.y - 1, box.width + 2, box.height + 2);
                g.setColor(Color.WHITE); g.fill(circle);
                g.setColor(accent); g.draw(circle);
                continue;
            }
            g.setColor(Color.WHITE); g.fill(box);
            g.setColor(accent); g.draw(box);
        }
    }

    private static BasicStroke dashed(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4, new float[]{5, 3}, 0);
    }
}
