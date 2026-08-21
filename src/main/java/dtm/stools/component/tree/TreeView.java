package dtm.stools.component.tree;

import dtm.stools.component.tree.event.EventTree;
import dtm.stools.component.tree.event.EventTreeView;
import lombok.Getter;
import lombok.Setter;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeView<T> extends TreeViewListener {

    protected static final DataFlavor TREE_NODE_ARRAY_FLAVOR = createTreeNodeArrayFlavor();

    protected static class PendingNodeUpdate<T> {
        protected final List<TreeNode<T>> nodes;
        protected final Consumer<TreeNode<T>> updater;

        protected PendingNodeUpdate(List<TreeNode<T>> nodes, Consumer<TreeNode<T>> updater) {
            this.nodes = nodes;
            this.updater = updater;
        }
    }

    @Getter
    protected TreeNode<T> rootNode;

    @Getter
    @Setter
    protected boolean rootVisible = true;

    @Getter
    protected TreeViewMode mode = TreeViewMode.SINGLE;

    @Getter
    @Setter
    protected boolean propagateCheckDown = true;

    @Getter
    @Setter
    protected boolean propagateCheckUp = true;

    @Getter
    @Setter
    protected boolean toggleCheckOnRowClick = false;

    @Getter
    @Setter
    protected boolean expandOnDoubleClick = true;

    @Getter
    @Setter
    protected boolean editOnF2 = true;

    @Getter
    @Setter
    protected boolean removeSelectedOnDelete = false;

    @Getter
    @Setter
    protected boolean activateOnEnter = true;

    @Getter
    @Setter
    protected boolean toggleCheckOnSpace = true;

    @Getter
    @Setter
    protected boolean expandCollapseOnArrowKeys = true;

    @Getter
    protected boolean dragAndDropEnabled = false;

    @Getter
    protected boolean externalDropEnabled = false;

    @Getter
    @Setter
    protected boolean dragImageEnabled = false;

    @Getter
    @Setter
    protected Function<TreePopupContext<T>, JPopupMenu> popupMenuProvider;

    @Getter @Setter
    protected Function<TreeDropContext<T>, Boolean> dropPolicy;

    @Getter @Setter
    protected Function<TreeDropContext<T>, Boolean> dropOverPolicy;

    @Getter @Setter
    protected Function<TreeExternalDropContext<T>, Boolean> externalDropHandler;

    @Getter @Setter
    protected Consumer<List<TreeNode<T>>> clipboardCopyHandler;

    @Getter @Setter
    protected Consumer<List<TreeNode<T>>> clipboardCutHandler;

    @Getter @Setter
    protected Consumer<TreeNode<T>> clipboardPasteHandler;

    @Getter @Setter
    protected Consumer<List<TreeNode<T>>> deleteHandler;

    @Getter @Setter
    protected DropHighlightPainter dropHighlightPainter;

    @Getter
    protected boolean dropHighlightAnimated = false;

    @Getter @Setter
    protected int dropHighlightAnimationPeriodMs = 1380;

    @Getter @Setter
    protected int dropHighlightFrameRateMs = 40;

    @Getter @Setter
    protected int checkBoxClickWidth = 22;

    @Getter @Setter
    protected int iconClickWidth = 22;

    protected long lastDoubleClickWhen = -1;
    protected TreePath lastDoubleClickPath;
    protected int hoveredRow = -1;
    protected List<TreeNode<T>> lastSelectedNodes = Collections.emptyList();
    protected List<TreeNode<T>> activeDraggedNodes = Collections.emptyList();
    protected boolean internalDragActive = false;

    protected transient Timer dropHighlightTimer;
    protected transient JPopupMenu activePopupMenu;

    protected int dropHighlightRow = -1;
    protected long dropHighlightStart;
    protected int lastEvaluatedDropRow = -1;
    protected boolean lastEvaluatedDropExternal;
    protected boolean lastEvaluatedDropAccepted;

    @Getter
    protected TreeFilter<T> filter;

    @Getter
    protected String searchTerm;

    @Getter @Setter
    protected boolean caseSensitiveSearch = false;

    protected final Map<String, List<TreeNode<T>>> filterSnapshot = new HashMap<>();
    protected final Set<String> expandedIds = new HashSet<>();
    protected final List<PendingNodeUpdate<T>> pendingNodeUpdates = new ArrayList<>();

    public TreeView() {
        this(new TreeNode<>(null, "root"));
    }

    protected static DataFlavor createTreeNodeArrayFlavor() {
        try {
            return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + TreeNode[].class.getName() + "\"");
        } catch (ClassNotFoundException e) {
            return new DataFlavor(TreeNode[].class, "TreeNodeArray");
        }
    }

    public TreeView(TreeNode<T> root) {
        super();
        this.rootNode = root;
        setModel(new DefaultTreeModel(this.rootNode, false));
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setRowHeight(24);
        setShowsRootHandles(true);
        setToggleClickCount(0);
        setEditable(false);
        putClientProperty("JTree.lineStyle", "Angled");
        ToolTipManager.sharedInstance().registerComponent(this);

        setCellRenderer(createDefaultRenderer());
        setCellEditor(createDefaultEditor());

        installListeners();
        installKeyBindings();
    }

    protected TreeNodeRenderer createDefaultRenderer() {
        return new TreeNodeRenderer();
    }

    protected TreeNodeEditor createDefaultEditor() {
        return new TreeNodeEditor();
    }

    public DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) getModel();
    }

    public void setRoot(TreeNode<T> root) {
        this.rootNode = root;
        getTreeModel().setRoot(root);
        clearFilterSnapshot();
        super.setRootVisible(rootVisible);
    }

    @Override
    public void setRootVisible(boolean rootVisible) {
        this.rootVisible = rootVisible;
        super.setRootVisible(rootVisible);
    }

    public void setMode(TreeViewMode mode) {
        if (mode == null) mode = TreeViewMode.SINGLE;
        this.mode = mode;
        switch (mode) {
            case SINGLE -> getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            case MULTIPLE -> getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
            case DISCONTIGUOUS -> getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        }
    }

    public void setShowCheckBoxPlaceholder(boolean show) {
        if (getCellRenderer() instanceof TreeNodeRenderer renderer) {
            renderer.setShowCheckBoxPlaceholder(show);
            repaint();
        }
    }

    public void setDragAndDropEnabled(boolean dragAndDropEnabled) {
        this.dragAndDropEnabled = dragAndDropEnabled;
        updateDragAndDropSupport();
    }

    public void setExternalDropEnabled(boolean externalDropEnabled) {
        this.externalDropEnabled = externalDropEnabled;
        updateDragAndDropSupport();
    }

    protected void updateDragAndDropSupport() {
        boolean enabled = dragAndDropEnabled || externalDropEnabled;
        setDragEnabled(dragAndDropEnabled);
        setDropMode(enabled ? DropMode.ON : DropMode.USE_SELECTION);
        setTransferHandler(enabled ? new TreeViewTransferHandler() : null);
    }

    public TreeNode<T> addNode(TreeNode<T> parent, T value) {
        TreeNode<T> child = new TreeNode<>(value);
        return addNode(parent, child);
    }

    public TreeNode<T> addNode(TreeNode<T> parent, TreeNode<T> child) {
        if (parent == null) parent = rootNode;
        Set<String> expansion = snapshotExpansion();
        parent.inheritProvidersTo(child);
        getTreeModel().insertNodeInto(child, parent, parent.getChildCount());
        restoreExpansion(expansion);
        dispatchTree(EventTreeView.NODE_ADD, child, null, child.getData(), null);
        return child;
    }

    public void removeNode(TreeNode<T> node) {
        if (node == null || node == rootNode) return;
        Set<String> expansion = snapshotExpansion();
        TreeNode<T> parent = (TreeNode<T>) node.getParent();
        Object oldData = node.getData();
        getTreeModel().removeNodeFromParent(node);
        restoreExpansion(expansion);
        dispatchTree(EventTreeView.NODE_REMOVE, parent, oldData, null, null);
    }

    public void moveNode(TreeNode<T> node, TreeNode<T> newParent, int index) {
        if (node == null || newParent == null || node == rootNode) return;
        Set<String> expansion = snapshotExpansion();
        TreeNode<T> oldParent = (TreeNode<T>) node.getParent();
        getTreeModel().removeNodeFromParent(node);
        int target = (index < 0 || index > newParent.getChildCount()) ? newParent.getChildCount() : index;
        getTreeModel().insertNodeInto(node, newParent, target);
        restoreExpansion(expansion);
        dispatchTree(EventTreeView.NODE_MOVE, node, oldParent, newParent, null);
    }

    public void refreshNode(TreeNode<T> node) {
        if (node == null) node = rootNode;
        Set<String> expansion = snapshotExpansion();
        getTreeModel().nodeStructureChanged(node);
        restoreExpansion(expansion);
    }

    public TreePath getPathForNode(TreeNode<T> node) {
        return node == null ? null : new TreePath(node.getPath());
    }

    public void selectNode(TreeNode<T> node) {
        if (node == null || !node.isSelectable()) return;
        TreePath path = getPathForNode(node);
        if (path != null) setSelectionPath(path);
    }

    public void selectNodes(Collection<? extends TreeNode<T>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            clearSelection();
            return;
        }
        List<TreePath> paths = new ArrayList<>();
        for (TreeNode<T> node : nodes) {
            if (node != null && node.isSelectable()) {
                TreePath path = getPathForNode(node);
                if (path != null) paths.add(path);
            }
        }
        setSelectionPaths(paths.toArray(new TreePath[0]));
    }

    public void revealNode(TreeNode<T> node) {
        if (node == null) return;
        TreePath path = getPathForNode(node);
        if (path == null) return;
        expandParents(node);
        scrollPathToVisible(path);
    }

    public void expandParents(TreeNode<T> node) {
        if (node == null) return;
        TreeNode<T> parent = (TreeNode<T>) node.getParent();
        List<TreeNode<T>> parents = new ArrayList<>();
        while (parent != null) {
            parents.add(parent);
            parent = (TreeNode<T>) parent.getParent();
        }
        for (int i = parents.size() - 1; i >= 0; i--) {
            expandPath(getPathForNode(parents.get(i)));
        }
    }

    public TreePath getPathAtMouse(MouseEvent e) {
        return e == null ? null : resolvePath(e, true);
    }

    public TreeNode<T> getNodeAtMouse(MouseEvent e) {
        return nodeFromPath(getPathAtMouse(e));
    }

    @SuppressWarnings("unchecked")
    public TreeNode<T> getSelectedNode() {
        TreePath path = getSelectionPath();
        if (path == null) return null;
        Object last = path.getLastPathComponent();
        return last instanceof TreeNode ? (TreeNode<T>) last : null;
    }

    @SuppressWarnings("unchecked")
    public List<TreeNode<T>> getSelectedNodes() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null) return Collections.emptyList();
        List<TreeNode<T>> out = new ArrayList<>(paths.length);
        for (TreePath p : paths) {
            Object last = p.getLastPathComponent();
            if (last instanceof TreeNode) out.add((TreeNode<T>) last);
        }
        return out;
    }

    public List<TreeNode<T>> getCheckedNodes() {
        return rootNode.findAll(n -> n.getCheckState() == CheckState.CHECKED);
    }

    public TreeNode<T> findById(String id) {
        if (id == null) return null;
        return rootNode.findFirst(n -> id.equals(n.getId()));
    }

    public TreeNode<T> findByData(Predicate<T> matcher) {
        return rootNode.findFirst(n -> {
            try { return matcher.test(n.getData()); } catch (Exception e) { return false; }
        });
    }

    public int updateNodes(Predicate<TreeNode<T>> matcher, Consumer<TreeNode<T>> updater) {
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(updater, "updater");

        List<TreeNode<T>> matches = new ArrayList<>();
        for (TreeNode<T> node : collectNodes()) {
            if (matcher.test(node)) matches.add(node);
        }
        if (matches.isEmpty()) return 0;

        Set<String> expansion = snapshotExpansion();
        for (TreeNode<T> node : matches) {
            updater.accept(node);
            getTreeModel().nodeChanged(node);
        }
        restoreExpansion(expansion);
        repaint();
        return matches.size();
    }

    public void queueUpdateNodes(Consumer<TreeNode<T>> updater) {
        Objects.requireNonNull(updater, "updater");
        List<TreeNode<T>> matches = new ArrayList<>(collectNodes());
        pendingNodeUpdates.add(new PendingNodeUpdate<>(List.copyOf(matches), updater));
    }

    public int queueUpdateNodes(Predicate<TreeNode<T>> matcher, Consumer<TreeNode<T>> updater) {
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(updater, "updater");

        List<TreeNode<T>> matches = new ArrayList<>();
        for (TreeNode<T> node : collectNodes()) {
            if (matcher.test(node)) matches.add(node);
        }
        if (matches.isEmpty()) return 0;

        pendingNodeUpdates.add(new PendingNodeUpdate<>(List.copyOf(matches), updater));
        return matches.size();
    }

    public int applyQueuedNodeUpdates() {
        if (pendingNodeUpdates.isEmpty()) return 0;

        List<PendingNodeUpdate<T>> updates = new ArrayList<>(pendingNodeUpdates);
        pendingNodeUpdates.clear();

        Set<String> expansion = snapshotExpansion();
        List<TreeNode<T>> changedNodes = new ArrayList<>();
        for (PendingNodeUpdate<T> update : updates) {
            for (TreeNode<T> node : update.nodes) {
                update.updater.accept(node);
                if (!changedNodes.contains(node)) changedNodes.add(node);
            }
        }
        for (TreeNode<T> node : changedNodes) {
            getTreeModel().nodeChanged(node);
        }
        restoreExpansion(expansion);
        repaint();
        return changedNodes.size();
    }

    public int getQueuedNodeUpdateCount() {
        return pendingNodeUpdates.size();
    }

    public void clearQueuedNodeUpdates() {
        pendingNodeUpdates.clear();
    }

    public int updateSubtree(TreeNode<T> from, Consumer<TreeNode<T>> updater) {
        return updateSubtree(from, node -> true, updater);
    }

    public int updateSubtree(TreeNode<T> from, Predicate<TreeNode<T>> matcher, Consumer<TreeNode<T>> updater) {
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(updater, "updater");
        if (from == null) return 0;

        List<TreeNode<T>> matches = new ArrayList<>();
        from.walk(node -> {
            if (matcher.test(node)) matches.add(node);
        });
        if (matches.isEmpty()) return 0;

        for (TreeNode<T> node : matches) {
            updater.accept(node);
            getTreeModel().nodeChanged(node);
        }
        repaint();
        return matches.size();
    }

    public boolean updateFirstNode(Predicate<TreeNode<T>> matcher, Consumer<TreeNode<T>> updater) {
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(updater, "updater");

        TreeNode<T> node = rootNode.findFirst(matcher);
        if (node == null) return false;

        Set<String> expansion = snapshotExpansion();
        updater.accept(node);
        getTreeModel().nodeChanged(node);
        restoreExpansion(expansion);
        repaint();
        return true;
    }

    public void expandAll() {
        expandAll(rootNode);
    }

    public void expandAll(TreeNode<T> from) {
        if (from == null) return;
        expandPath(new TreePath(from.getPath()));
        for (TreeNode<T> c : from.getChildrenList()) expandAll(c);
    }

    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0; i--) collapseRow(i);
    }

    public void expandToDepth(int depth) {
        expandToDepth(rootNode, 0, depth);
    }

    protected void expandToDepth(TreeNode<T> node, int current, int max) {
        if (node == null || current > max) return;
        expandPath(new TreePath(node.getPath()));
        if (current == max) return;
        for (TreeNode<T> c : node.getChildrenList()) expandToDepth(c, current + 1, max);
    }

    public void expandTo(TreeNode<T> node) {
        if (node == null) return;
        scrollPathToVisible(new TreePath(node.getPath()));
    }

    public Set<String> snapshotExpansion() {
        syncExpandedIdsFromTree();
        return new HashSet<>(expandedIds);
    }

    public void restoreExpansion(Set<String> ids) {
        if (ids == null) return;
        Set<String> targetIds = new HashSet<>(ids);
        List<TreeNode<T>> nodes = collectNodes();

        for (int i = nodes.size() - 1; i >= 0; i--) {
            TreeNode<T> node = nodes.get(i);
            TreePath path = getPathForNode(node);
            if (path != null && isExpanded(path) && !targetIds.contains(node.getId())) {
                collapsePath(path);
            }
        }

        for (TreeNode<T> node : nodes) {
            if (targetIds.contains(node.getId())) {
                TreePath path = getPathForNode(node);
                if (path != null) expandPath(path);
            }
        }
        syncExpandedIdsFromTree();
    }

    protected List<TreeNode<T>> collectNodes() {
        List<TreeNode<T>> nodes = new ArrayList<>();
        if (rootNode != null) rootNode.walk(nodes::add);
        return nodes;
    }

    protected void syncExpandedIdsFromTree() {
        expandedIds.clear();
        for (TreeNode<T> node : collectNodes()) {
            TreePath path = getPathForNode(node);
            if (path != null && isExpanded(path)) {
                expandedIds.add(node.getId());
            }
        }
    }

    public void setFilter(TreeFilter<T> filter) {
        this.filter = filter;
        applyFilter();
        dispatchTree(EventTreeView.FILTER_CHANGE, rootNode, null, filter, null);
    }

    public void clearFilter() {
        this.filter = null;
        this.searchTerm = null;
        restoreFilterSnapshot();
        dispatchTree(EventTreeView.FILTER_CHANGE, rootNode, null, null, null);
    }

    public void search(String term) {
        this.searchTerm = term;
        if (term == null || term.isEmpty()) {
            clearFilter();
            return;
        }
        String needle = caseSensitiveSearch ? term : term.toLowerCase();
        setFilter(node -> {
            String label = node.computeLabel();
            if (label == null) return false;
            String hay = caseSensitiveSearch ? label : label.toLowerCase();
            return hay.contains(needle);
        });
    }

    public List<TreeNode<T>> findMatches(String term) {
        if (term == null || term.isEmpty()) return Collections.emptyList();
        String needle = caseSensitiveSearch ? term : term.toLowerCase();
        return rootNode.findAll(n -> {
            String label = n.computeLabel();
            if (label == null) return false;
            String hay = caseSensitiveSearch ? label : label.toLowerCase();
            return hay.contains(needle);
        });
    }

    protected void applyFilter() {
        if (filter == null) {
            restoreFilterSnapshot();
            return;
        }
        if (filterSnapshot.isEmpty()) {
            rootNode.walk(n -> filterSnapshot.put(n.getId(), n.getChildrenList()));
        }
        rootNode.walk(n -> {
            List<TreeNode<T>> orig = filterSnapshot.get(n.getId());
            if (orig != null) {
                n.removeAllChildren();
                for (TreeNode<T> c : orig) n.add(c);
            }
        });
        Set<String> keep = new HashSet<>();
        markMatchingPath(rootNode, keep);
        pruneByKeepSet(rootNode, keep);
        getTreeModel().nodeStructureChanged(rootNode);
        rootNode.walk(n -> {
            if (keep.contains(n.getId()) && n.getChildCount() > 0) {
                expandPath(new TreePath(n.getPath()));
            }
        });
    }

    protected boolean markMatchingPath(TreeNode<T> node, Set<String> keep) {
        boolean self = false;
        try { self = filter.accept(node); } catch (Exception ignored) {}
        boolean anyChild = false;
        for (TreeNode<T> c : node.getChildrenList()) {
            if (markMatchingPath(c, keep)) anyChild = true;
        }
        boolean visible = self || anyChild;
        if (visible) keep.add(node.getId());
        return visible;
    }

    protected void pruneByKeepSet(TreeNode<T> node, Set<String> keep) {
        List<TreeNode<T>> children = node.getChildrenList();
        for (TreeNode<T> c : children) {
            if (!keep.contains(c.getId())) {
                node.remove(c);
            } else {
                pruneByKeepSet(c, keep);
            }
        }
    }

    protected void restoreFilterSnapshot() {
        if (filterSnapshot.isEmpty()) return;
        rootNode.walk(n -> {
            List<TreeNode<T>> orig = filterSnapshot.get(n.getId());
            if (orig != null) {
                n.removeAllChildren();
                for (TreeNode<T> c : orig) n.add(c);
            }
        });
        getTreeModel().nodeStructureChanged(rootNode);
        clearFilterSnapshot();
    }

    protected void clearFilterSnapshot() {
        filterSnapshot.clear();
    }

    public void loadChildren(TreeNode<T> node) {
        if (node == null) return;
        TreeNodeProvider<T> provider = node.getChildrenProvider();
        if (provider == null) {
            node.markLoaded();
            return;
        }
        if (provider.isAsync()) {
            loadChildrenAsync(node, provider);
        } else {
            loadChildrenSync(node, provider);
        }
    }

    protected void loadChildrenSync(TreeNode<T> node, TreeNodeProvider<T> provider) {
        try {
            List<TreeNode<T>> children = provider.getChildren(node);
            applyLoadedChildren(node, children);
        } catch (Exception e) {
            handleLoadError(node, e);
        }
    }

    protected void loadChildrenAsync(TreeNode<T> node, TreeNodeProvider<T> provider) {
        node.setLoading(true);
        getTreeModel().nodeChanged(node);
        new SwingWorker<List<TreeNode<T>>, Void>() {
            @Override protected List<TreeNode<T>> doInBackground() throws Exception {
                return provider.getChildren(node);
            }

            @Override protected void done() {
                try {
                    applyLoadedChildren(node, get());
                } catch (Exception e) {
                    handleLoadError(node, e);
                }
            }
        }.execute();
    }

    protected void applyLoadedChildren(TreeNode<T> node, List<TreeNode<T>> children) {
        Set<String> expansion = snapshotExpansion();
        node.setLoading(false);
        node.removeAllChildren();
        if (children != null) {
            for (TreeNode<T> c : children) {
                node.inheritProvidersTo(c);
                node.add(c);
            }
        }
        node.markLoaded();
        getTreeModel().nodeStructureChanged(node);
        restoreExpansion(expansion);
        dispatchTree(EventTreeView.NODE_LAZY_LOAD, node, null, children, null);
    }

    protected void handleLoadError(TreeNode<T> node, Exception e) {
        node.setLoading(false);
        node.markLoaded();
        getTreeModel().nodeChanged(node);
        e.printStackTrace();
    }

    public void setNodeCheckState(TreeNode<T> node, CheckState state) {
        if (node == null) return;
        CheckState old = node.getCheckState();
        node.setCheckState(state);
        List<TreeNode<T>> changed = new ArrayList<>();
        changed.add(node);
        if (propagateCheckDown && state != CheckState.INDETERMINATE) {
            node.walk(child -> {
                if (child != node && child.isCheckable()) {
                    child.setCheckState(state);
                    changed.add(child);
                }
            });
        }
        if (propagateCheckUp) {
            propagateUp(node, changed);
        }
        for (TreeNode<T> changedNode : changed) {
            getTreeModel().nodeChanged(changedNode);
        }
        onCheckChanged(node, old, state);
        dispatchTreeCheck(node, old, state);
    }

    protected void propagateUp(TreeNode<T> node, List<TreeNode<T>> changed) {
        TreeNode<T> parent = (TreeNode<T>) node.getParent();
        while (parent != null) {
            int checked = 0, unchecked = 0, indet = 0, total = parent.getChildCount();
            for (TreeNode<T> c : parent.getChildrenList()) {
                switch (c.getCheckState()) {
                    case CHECKED -> checked++;
                    case UNCHECKED -> unchecked++;
                    case INDETERMINATE -> indet++;
                }
            }
            CheckState newState;
            if (indet > 0 || (checked > 0 && unchecked > 0)) newState = CheckState.INDETERMINATE;
            else if (checked == total) newState = CheckState.CHECKED;
            else newState = CheckState.UNCHECKED;
            if (parent.isCheckable()) {
                parent.setCheckState(newState);
                changed.add(parent);
            }
            parent = (TreeNode<T>) parent.getParent();
        }
    }

    protected void onCheckChanged(TreeNode<T> node, CheckState oldState, CheckState newState) {
    }

    protected boolean onNodeClicked(TreeNode<T> node, MouseEvent e) {
        return false;
    }

    protected void installListeners() {
        addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                List<TreeNode<T>> selected = getSelectedNodes();
                List<TreePath> blocked = new ArrayList<>();
                for (TreeNode<T> selectedNode : selected) {
                    if (!selectedNode.isSelectable()) blocked.add(getPathForNode(selectedNode));
                }
                if (!blocked.isEmpty()) {
                    for (TreePath path : blocked) removeSelectionPath(path);
                    return;
                }
                if (!lastSelectedNodes.equals(selected)) {
                    List<TreeNode<T>> oldSelected = lastSelectedNodes;
                    lastSelectedNodes = List.copyOf(selected);
                    dispatchTree(EventTreeView.SELECTION_CHANGE, getSelectedNode(), oldSelected, lastSelectedNodes, null);
                }
                TreeNode<T> node = getSelectedNode();
                dispatchTree(EventTreeView.NODE_SELECT, node, null, node, null);
            }
        });

        addTreeWillExpandListener(new TreeWillExpandListener() {
            @SuppressWarnings("unchecked")
            @Override public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof TreeNode) {
                    TreeNode<T> node = (TreeNode<T>) last;
                    if (node.isLazy() && !node.isLoaded()) {
                        loadChildren(node);
                    }
                }
            }

            @Override public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });

        addTreeExpansionListener(new TreeExpansionListener() {
            @SuppressWarnings("unchecked")
            @Override public void treeExpanded(TreeExpansionEvent event) {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof TreeNode) {
                    TreeNode<T> node = (TreeNode<T>) last;
                    expandedIds.add(node.getId());
                    dispatchTree(EventTreeView.NODE_EXPAND, node, null, node, null);
                }
            }

            @SuppressWarnings("unchecked")
            @Override public void treeCollapsed(TreeExpansionEvent event) {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof TreeNode) {
                    TreeNode<T> node = (TreeNode<T>) last;
                    node.walk(n -> expandedIds.remove(n.getId()));
                    dispatchTree(EventTreeView.NODE_COLLAPSE, node, null, node, null);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    handleDoubleClick(e);
                    return;
                }

                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    handlePopupRequest(e);
                    return;
                }

                TreePath path = resolvePath(e, false);
                TreeNode<T> node = nodeFromPath(path);
                if (node == null) return;
                if (!node.isEnabled()) {
                    e.consume();
                    return;
                }

                if (node.isCheckable()
                        && SwingUtilities.isLeftMouseButton(e)
                        && e.getClickCount() == 1
                        && isCheckToggleHit(path, e)) {
                    dispatchTree(EventTreeView.NODE_CHECKBOX_CLICK, node, null, node, null, e);
                    toggleCheck(node);
                    e.consume();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    dispatchTree(EventTreeView.NODE_CLICK, node, null, node, null, e);
                    if (isIconHit(path, e)) {
                        dispatchTree(EventTreeView.NODE_ICON_CLICK, node, null, node, null, e);
                    }
                }

                if (onNodeClicked(node, e)) {
                    e.consume();
                }
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    handleDoubleClick(e);
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopupRequest(e);
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                updateHoveredRow(-1);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                updateHoveredRow(getRowForLocation(e.getX(), e.getY()));
            }
        });

        getTreeModel().addTreeModelListener(new javax.swing.event.TreeModelListener() {
            @Override public void treeNodesChanged(javax.swing.event.TreeModelEvent e) {
                Object[] children = e.getChildren();
                if (children != null && children.length > 0 && children[0] instanceof TreeNode<?>) {
                    @SuppressWarnings("unchecked")
                    TreeNode<T> node = (TreeNode<T>) children[0];
                    dispatchTree(EventTreeView.NODE_EDIT, node, null, node.getData(), null);
                }
            }

            @Override public void treeNodesInserted(javax.swing.event.TreeModelEvent e) {
            }

            @Override public void treeNodesRemoved(javax.swing.event.TreeModelEvent e) {
            }

            @Override public void treeStructureChanged(javax.swing.event.TreeModelEvent e) {
            }
        });
    }

    protected void installKeyBindings() {
        InputMap input = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actions = getActionMap();

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "treeview.rename");
        actions.put("treeview.rename", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (editOnF2) editSelectedNode();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "treeview.delete");
        actions.put("treeview.delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (deleteHandler != null) {
                    List<TreeNode<T>> selected = getSelectedNodes();
                    if (!selected.isEmpty()) deleteHandler.accept(List.copyOf(selected));
                } else if (removeSelectedOnDelete) {
                    removeSelectedNodes();
                }
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "treeview.activate");
        actions.put("treeview.activate", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (activateOnEnter) activateSelectedNode();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "treeview.check");
        actions.put("treeview.check", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (toggleCheckOnSpace) toggleSelectedCheck();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "treeview.expand");
        actions.put("treeview.expand", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (expandCollapseOnArrowKeys) expandSelectedNode();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "treeview.collapse");
        actions.put("treeview.collapse", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (expandCollapseOnArrowKeys) collapseOrSelectParent();
            }
        });

        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), "treeview.clipboard.copy");
        actions.put("treeview.clipboard.copy", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (clipboardCopyHandler == null) return;
                List<TreeNode<T>> selected = getSelectedNodes();
                if (!selected.isEmpty()) clipboardCopyHandler.accept(List.copyOf(selected));
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), "treeview.clipboard.cut");
        actions.put("treeview.clipboard.cut", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (clipboardCutHandler == null) return;
                List<TreeNode<T>> selected = getSelectedNodes();
                if (!selected.isEmpty()) clipboardCutHandler.accept(List.copyOf(selected));
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), "treeview.clipboard.paste");
        actions.put("treeview.clipboard.paste", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (clipboardPasteHandler == null) return;
                TreeNode<T> node = getSelectedNode();
                if (node != null) clipboardPasteHandler.accept(node);
            }
        });
    }

    public void editSelectedNode() {
        TreeNode<T> node = getSelectedNode();
        if (node == null || !node.isEditable() || !isEditable()) return;
        startEditingAtPath(getPathForNode(node));
    }

    public void removeSelectedNodes() {
        List<TreeNode<T>> selected = new ArrayList<>(getSelectedNodes());
        selected.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
        for (TreeNode<T> node : selected) {
            removeNode(node);
        }
    }

    public void activateSelectedNode() {
        TreeNode<T> node = getSelectedNode();
        if (node == null || !node.isEnabled()) return;
        dispatchTree(EventTreeView.NODE_ACTIVATE, node, null, node, null);
        if (expandOnDoubleClick && node.hasChildren()) {
            TreePath path = getPathForNode(node);
            if (isExpanded(path)) collapsePath(path); else expandPath(path);
        }
    }

    public void toggleSelectedCheck() {
        TreeNode<T> node = getSelectedNode();
        if (node != null && node.isEnabled() && node.isCheckable()) {
            toggleCheck(node);
        }
    }

    public void expandSelectedNode() {
        TreeNode<T> node = getSelectedNode();
        if (node == null || !node.hasChildren()) return;
        expandPath(getPathForNode(node));
    }

    public void collapseOrSelectParent() {
        TreeNode<T> node = getSelectedNode();
        if (node == null) return;
        TreePath path = getPathForNode(node);
        if (isExpanded(path)) {
            collapsePath(path);
            return;
        }
        TreeNode<T> parent = (TreeNode<T>) node.getParent();
        if (parent != null && parent.isSelectable()) selectNode(parent);
    }

    protected List<TreeNode<T>> getDraggableSelectedNodes() {
        List<TreeNode<T>> selected = getSelectedNodes();
        List<TreeNode<T>> draggable = new ArrayList<>();
        for (TreeNode<T> node : selected) {
            if (canDragNode(node) && !hasSelectedAncestor(node, selected)) {
                draggable.add(node);
            }
        }
        return draggable;
    }

    protected boolean canDragNode(TreeNode<T> node) {
        return node != null && node != rootNode && node.isEnabled() && node.isDraggable() && node.getParent() != null;
    }

    protected boolean canDropNode(TreeNode<T> node, TreeNode<T> parent) {
        return canDragNode(node)
                && parent != null
                && parent.isEnabled()
                && parent.isDropTarget()
                && node != parent
                && !isAncestorOf(node, parent);
    }

    protected boolean hasSelectedAncestor(TreeNode<T> node, List<TreeNode<T>> selected) {
        TreeNode<T> parent = (TreeNode<T>) node.getParent();
        while (parent != null) {
            if (selected.contains(parent)) return true;
            parent = (TreeNode<T>) parent.getParent();
        }
        return false;
    }

    protected boolean isAncestorOf(TreeNode<T> ancestor, TreeNode<T> node) {
        TreeNode<T> parent = node;
        while (parent != null) {
            if (parent == ancestor) return true;
            parent = (TreeNode<T>) parent.getParent();
        }
        return false;
    }

    protected boolean moveNodeByDrop(TreeNode<T> node, TreeNode<T> newParent, int index) {
        if (!canDropNode(node, newParent)) return false;
        Set<String> expansion = snapshotExpansion();
        TreeNode<T> oldParent = (TreeNode<T>) node.getParent();
        int oldIndex = oldParent.getIndex(node);
        int target = index < 0 ? newParent.getChildCount() : Math.min(index, newParent.getChildCount());
        if (oldParent == newParent && oldIndex < target) target--;
        getTreeModel().removeNodeFromParent(node);
        target = Math.max(0, Math.min(target, newParent.getChildCount()));
        getTreeModel().insertNodeInto(node, newParent, target);
        restoreExpansion(expansion);
        dispatchTree(EventTreeView.NODE_MOVE, node, oldParent, newParent, null);
        return true;
    }

    protected DropTarget resolveDropTarget(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) return null;
        if (!(support.getDropLocation() instanceof javax.swing.JTree.DropLocation location)) return null;
        TreePath path = location.getPath();
        if (path == null) {
            Point p = location.getDropPoint();
            if (p != null) path = getClosestPathForLocation(p.x, p.y);
        }
        TreeNode<T> parent = nodeFromPath(path);
        if (parent == null) return null;
        int index = location.getChildIndex();
        if (index < 0) index = parent.getChildCount();
        return new DropTarget(parent, index);
    }

    protected TreeDropContext<T> createDropContext(TreeNode<?>[] nodes, DropTarget target) {
        List<TreeNode<T>> typedNodes = new ArrayList<>();
        for (TreeNode<?> node : nodes) {
            typedNodes.add((TreeNode<T>) node);
        }
        return new TreeDropContext<>(this, List.copyOf(typedNodes), (TreeNode<T>) target.parent(), target.index());
    }

    protected Image createDragImage(List<TreeNode<T>> nodes) {
        Rectangle bounds = null;
        for (TreeNode<T> node : nodes) {
            Rectangle nodeBounds = getPathBounds(getPathForNode(node));
            if (nodeBounds == null) continue;
            bounds = bounds == null ? new Rectangle(nodeBounds) : bounds.union(nodeBounds);
        }
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return null;

        int width = Math.min(Math.max(bounds.width + 8, 96), 360);
        int height = Math.min(bounds.height + 4, 180);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.82f));
            g2.translate(-bounds.x + 4, -bounds.y + 2);
            g2.setClip(bounds.x - 4, bounds.y - 2, width, height);
            paint(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    protected TreeExternalDropContext<T> createExternalDropContext(TransferHandler.TransferSupport support, DropTarget target) {
        return new TreeExternalDropContext<>(
                this,
                (TreeNode<T>) target.parent(),
                target.index(),
                support.getTransferable(),
                support.getDataFlavors()
        );
    }

    protected boolean isDropAcceptedByPolicy(TreeDropContext<T> context) {
        if (dropPolicy == null) return true;
        try {
            return Boolean.TRUE.equals(dropPolicy.apply(context));
        } catch (Exception ignored) {
            return false;
        }
    }

    protected boolean isDropAcceptedByOverPolicy(TreeDropContext<T> context) {
        Function<TreeDropContext<T>, Boolean> policy = dropOverPolicy != null ? dropOverPolicy : dropPolicy;
        if (policy == null) return true;
        try {
            return Boolean.TRUE.equals(policy.apply(context));
        } catch (Exception ignored) {
            return false;
        }
    }

    protected List<TreeNode<T>> getInternalTransferNodes(TransferHandler.TransferSupport support) {
        if (!activeDraggedNodes.isEmpty()) return activeDraggedNodes;
        try {
            TreeNode<?>[] nodes = (TreeNode<?>[]) support.getTransferable().getTransferData(TREE_NODE_ARRAY_FLAVOR);
            List<TreeNode<T>> typedNodes = new ArrayList<>();
            if (nodes != null) {
                for (TreeNode<?> node : nodes) typedNodes.add((TreeNode<T>) node);
            }
            return typedNodes;
        } catch (UnsupportedFlavorException | IOException | ClassCastException e) {
            return Collections.emptyList();
        }
    }

    protected int rowForNode(TreeNode<T> node) {
        if (node == null) return -1;
        TreePath path = getPathForNode(node);
        return path == null ? -1 : getRowForPath(path);
    }

    protected void setDropHighlight(int row) {
        if (dropHighlightRow == row) return;
        int old = dropHighlightRow;
        dropHighlightRow = row;
        if (row >= 0) {
            if (old < 0) dropHighlightStart = System.currentTimeMillis();
            if (dropHighlightAnimated) {
                ensureDropHighlightTimer();
                if (!dropHighlightTimer.isRunning()) dropHighlightTimer.start();
            }
        } else if (dropHighlightTimer != null && dropHighlightTimer.isRunning()) {
            dropHighlightTimer.stop();
        }
        if (old >= 0) repaintHighlightRow(old);
        if (row >= 0) repaintHighlightRow(row);
    }

    public void setDropHighlightAnimated(boolean animated) {
        this.dropHighlightAnimated = animated;
        if (!animated && dropHighlightTimer != null && dropHighlightTimer.isRunning()) {
            dropHighlightTimer.stop();
        } else if (animated && dropHighlightRow >= 0) {
            ensureDropHighlightTimer();
            if (!dropHighlightTimer.isRunning()) dropHighlightTimer.start();
        }
    }

    protected void clearDropHighlight() {
        setDropHighlight(-1);
    }

    protected void resetDropEvaluationCache() {
        lastEvaluatedDropRow = -1;
        lastEvaluatedDropExternal = false;
        lastEvaluatedDropAccepted = false;
    }

    protected void ensureDropHighlightTimer() {
        if (dropHighlightTimer != null) return;
        dropHighlightTimer = new Timer(Math.max(10, dropHighlightFrameRateMs), e -> {
            if (dropHighlightRow >= 0) repaintHighlightRow(dropHighlightRow);
        });
        dropHighlightTimer.setRepeats(true);
    }

    protected void repaintHighlightRow(int row) {
        Rectangle b = getRowBounds(row);
        if (b == null) return;
        int width = Math.max(b.width, getVisibleRect().width);
        repaint(b.x - 3, b.y - 3, width + 6, b.height + 6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dropHighlightRow < 0) return;
        Rectangle b = getRowBounds(dropHighlightRow);
        if (b == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double phase = 1.0;
            if (dropHighlightAnimated) {
                long elapsed = System.currentTimeMillis() - dropHighlightStart;
                double period = Math.max(60, dropHighlightAnimationPeriodMs);
                phase = (Math.sin(elapsed * (2.0 * Math.PI) / period) + 1.0) / 2.0;
            }
            Rectangle area = computeDropHighlightArea(b);
            if (dropHighlightPainter != null) {
                dropHighlightPainter.paint(g2, this, area, phase);
            } else {
                paintDropHighlight(g2, area, phase);
            }
        } finally {
            g2.dispose();
        }
    }

    protected Rectangle computeDropHighlightArea(Rectangle rowBounds) {
        int width = Math.max(rowBounds.width, getVisibleRect().width - rowBounds.x - 4);
        return new Rectangle(rowBounds.x, rowBounds.y, width, rowBounds.height - 1);
    }

    protected void paintDropHighlight(Graphics2D g2, Rectangle area, double phase) {
        Color accent = UIManager.getColor("Tree.selectionBackground");
        if (accent == null) accent = new Color(57, 105, 138);
        int fillAlpha = (int) (40 + 70 * phase);
        int borderAlpha = (int) (150 + 90 * phase);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), fillAlpha));
        g2.fillRoundRect(area.x, area.y, area.width, area.height, 10, 10);
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));
        g2.drawRoundRect(area.x, area.y, area.width, area.height, 10, 10);
    }

    @FunctionalInterface
    public interface DropHighlightPainter {
        void paint(Graphics2D g2, JTree tree, Rectangle area, double phase);
    }

    protected record DropTarget(TreeNode parent, int index) {
    }

    protected class TreeViewTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            List<TreeNode<T>> nodes = getDraggableSelectedNodes();
            if (nodes.isEmpty()) return null;
            activeDraggedNodes = List.copyOf(nodes);
            updateHoveredRow(-1);
            internalDragActive = true;
            if (dragImageEnabled) {
                Image image = createDragImage(nodes);
                if (image != null) {
                    setDragImage(image);
                    setDragImageOffset(new Point(12, 12));
                }
            } else {
                setDragImage(null);
            }
            dispatchTree(EventTreeView.NODE_DRAG_START, nodes.get(0), null, List.copyOf(nodes), null);
            return new TreeNodeTransferable(nodes.toArray(new TreeNode[0]));
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            try {
                if (data == null) return;
                TreeNode<?>[] nodes = (TreeNode<?>[]) data.getTransferData(TREE_NODE_ARRAY_FLAVOR);
                if (nodes != null && nodes.length > 0) {
                    dispatchTree(EventTreeView.NODE_DRAG_END, (TreeNode<T>) nodes[0], List.of(nodes), transferActionName(action), null);
                }
            } catch (UnsupportedFlavorException | IOException | ClassCastException ignored) {
            } finally {
                activeDraggedNodes = Collections.emptyList();
                internalDragActive = false;
                setDragImage(null);
                clearDropHighlight();
                resetDropEvaluationCache();
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                resetDropEvaluationCache();
                clearDropHighlight();
                return false;
            }
            DropTarget target = resolveDropTarget(support);
            if (target == null) {
                resetDropEvaluationCache();
                clearDropHighlight();
                return false;
            }
            support.setShowDropLocation(false);
            boolean external = !support.isDataFlavorSupported(TREE_NODE_ARRAY_FLAVOR);
            try {
                support.setDropAction(external ? COPY : MOVE);
            } catch (Exception ignored) {
            }
            int targetRow = rowForNode((TreeNode<T>) target.parent());
            boolean accepted;
            if (targetRow >= 0 && targetRow == lastEvaluatedDropRow && external == lastEvaluatedDropExternal) {
                accepted = lastEvaluatedDropAccepted;
            } else {
                accepted = isDropTargetAccepted(support, target, external);
                lastEvaluatedDropRow = targetRow;
                lastEvaluatedDropExternal = external;
                lastEvaluatedDropAccepted = accepted;
            }
            if (accepted) {
                setDropHighlight(targetRow);
            } else {
                clearDropHighlight();
            }
            return true;
        }

        protected boolean isDropTargetAccepted(TransferSupport support, DropTarget target, boolean external) {
            if (external) {
                return externalDropEnabled
                        && externalDropHandler != null
                        && target.parent().isEnabled()
                        && target.parent().isDropTarget();
            }
            if (!dragAndDropEnabled) return false;
            List<TreeNode<T>> nodes = getInternalTransferNodes(support);
            if (nodes.isEmpty()) return false;
            for (TreeNode<T> node : nodes) {
                if (!canDropNode(node, (TreeNode<T>) target.parent())) return false;
            }
            TreeDropContext<T> context = new TreeDropContext<>(TreeView.this, List.copyOf(nodes), (TreeNode<T>) target.parent(), target.index());
            return isDropAcceptedByOverPolicy(context);
        }

        @Override
        public boolean importData(TransferSupport support) {
            DropTarget target = resolveDropTarget(support);
            if (target == null) return false;
            if (!support.isDataFlavorSupported(TREE_NODE_ARRAY_FLAVOR)) {
                TreeExternalDropContext<T> context = createExternalDropContext(support, target);
                boolean accepted = false;
                if (externalDropEnabled && externalDropHandler != null && target.parent().isEnabled() && target.parent().isDropTarget()) {
                    try {
                        support.setDropAction(COPY);
                        accepted = Boolean.TRUE.equals(externalDropHandler.apply(context));
                    } catch (Exception ignored) {
                        accepted = false;
                    }
                }
                dispatchTree(
                        accepted ? EventTreeView.NODE_EXTERNAL_DROP : EventTreeView.NODE_EXTERNAL_DROP_REJECT,
                        context.targetNode(),
                        null,
                        context,
                        null
                );
                return accepted;
            }
            try {
                List<TreeNode<T>> nodes = getInternalTransferNodes(support);
                TreeDropContext<T> context = new TreeDropContext<>(TreeView.this, List.copyOf(nodes), (TreeNode<T>) target.parent(), target.index());
                if (!isDropTargetAccepted(support, target, false)) {
                    dispatchTree(EventTreeView.NODE_DROP_REJECT, context.draggedNodes().isEmpty() ? null : context.draggedNodes().get(0), null, context, null);
                    return false;
                }
                if (!isDropAcceptedByPolicy(context)) {
                    dispatchTree(EventTreeView.NODE_DROP_REJECT, context.draggedNodes().isEmpty() ? null : context.draggedNodes().get(0), null, context, null);
                    return false;
                }
                support.setDropAction(MOVE);
                int index = target.index();
                List<TreeNode<T>> moved = new ArrayList<>();
                for (TreeNode<T> node : nodes) {
                    if (moveNodeByDrop(node, (TreeNode<T>) target.parent(), index)) {
                        moved.add(node);
                        index++;
                    }
                }
                if (!moved.isEmpty()) {
                    expandPath(getPathForNode((TreeNode<T>) target.parent()));
                    selectNodes(moved);
                    revealNode(moved.get(moved.size() - 1));
                }
                if (!moved.isEmpty()) {
                    dispatchTree(EventTreeView.NODE_DROP, (TreeNode<T>) target.parent(), null, new TreeDropContext<>(TreeView.this, List.copyOf(moved), (TreeNode<T>) target.parent(), target.index()), null);
                }
                return !moved.isEmpty();
            } catch (ClassCastException e) {
                return false;
            }
        }
    }

    protected static class TreeNodeTransferable implements Transferable {
        private final TreeNode<?>[] nodes;

        protected TreeNodeTransferable(TreeNode<?>[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{TREE_NODE_ARRAY_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return TREE_NODE_ARRAY_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return nodes;
        }
    }

    protected String transferActionName(int action) {
        return switch (action) {
            case TransferHandler.MOVE -> "MOVE";
            case TransferHandler.COPY -> "COPY";
            case TransferHandler.LINK -> "LINK";
            default -> "NONE";
        };
    }

    protected TreePath resolvePath(MouseEvent e, boolean allowFullRow) {
        int row = allowFullRow ? getClosestRowForLocation(e.getX(), e.getY()) : getRowForLocation(e.getX(), e.getY());
        if (row < 0) return null;
        Rectangle rowBounds = getRowBounds(row);
        if (rowBounds == null || e.getY() < rowBounds.y || e.getY() >= rowBounds.y + rowBounds.height) return null;
        return getPathForRow(row);
    }

    protected void updateHoveredRow(int row) {
        if (internalDragActive) return;
        if (hoveredRow == row) return;
        int old = hoveredRow;
        hoveredRow = row;
        putClientProperty("TreeView.hoveredRow", hoveredRow);
        repaintHoveredRow(old);
        repaintHoveredRow(hoveredRow);
    }

    private void repaintHoveredRow(int row) {
        if (row < 0) return;
        Rectangle bounds = getRowBounds(row);
        if (bounds == null) {
            repaint();
            return;
        }
        repaint(bounds);
    }

    @SuppressWarnings("unchecked")
    protected TreeNode<T> nodeFromPath(TreePath path) {
        if (path == null) return null;
        Object last = path.getLastPathComponent();
        return last instanceof TreeNode ? (TreeNode<T>) last : null;
    }

    protected boolean isCheckBoxHit(TreePath path, MouseEvent e) {
        TreeNode<T> node = nodeFromPath(path);
        if (node == null || !node.isCheckable()) return false;
        Rectangle bounds = getPathBounds(path);
        return bounds != null && e.getX() >= bounds.x && e.getX() < bounds.x + checkBoxClickWidth;
    }

    protected boolean isCheckToggleHit(TreePath path, MouseEvent e) {
        if (isCheckBoxHit(path, e)) return true;
        if (!toggleCheckOnRowClick) return false;
        Rectangle bounds = getPathBounds(path);
        return bounds != null && e.getX() >= bounds.x && e.getX() < bounds.x + bounds.width;
    }

    protected boolean isIconHit(TreePath path, MouseEvent e) {
        TreeNode<T> node = nodeFromPath(path);
        if (node == null) return false;
        Rectangle bounds = getPathBounds(path);
        if (bounds == null) return false;
        int iconStart = bounds.x + (node.isCheckable() ? checkBoxClickWidth : 0);
        return e.getX() >= iconStart && e.getX() < iconStart + iconClickWidth;
    }

    protected void handleDoubleClick(MouseEvent e) {
        TreePath path = resolvePath(e, true);
        TreeNode<T> node = nodeFromPath(path);
        if (node == null || !node.isEnabled() || isCheckBoxHit(path, e)) return;
        if (path.equals(lastDoubleClickPath) && e.getWhen() - lastDoubleClickWhen >= 0 && e.getWhen() - lastDoubleClickWhen < 250) {
            e.consume();
            return;
        }

        lastDoubleClickWhen = e.getWhen();
        lastDoubleClickPath = path;
        setSelectionPath(path);
        dispatchTree(EventTreeView.NODE_DOUBLE_CLICK, node, null, node, null, e);
        dispatchTree(EventTreeView.NODE_ACTIVATE, node, null, node, null, e);
        if (expandOnDoubleClick && node.hasChildren()) {
            if (isExpanded(path)) collapsePath(path); else expandPath(path);
        }
        e.consume();
    }

    protected void handlePopupRequest(MouseEvent e) {
        TreePath path = resolvePath(e, true);
        TreeNode<T> node = nodeFromPath(path);
        if (node == null) node = getSelectedNode();
        if (node == null || !node.isEnabled()) return;
        if (path != null && !isPathSelected(path)) setSelectionPath(path);
        dispatchTree(EventTreeView.NODE_RIGHT_CLICK, node, null, node, null, e);
        dispatchTree(EventTreeView.NODE_CONTEXT_MENU, node, null, node, null, e);
        showPopup(node, e);
        e.consume();
    }

    protected void toggleCheck(TreeNode<T> node) {
        CheckState next = node.getCheckState() == CheckState.CHECKED ? CheckState.UNCHECKED : CheckState.CHECKED;
        setNodeCheckState(node, next);
    }

    protected void showPopup(TreeNode<T> node, MouseEvent e) {
        if (activePopupMenu != null && activePopupMenu.isVisible()) {
            e.consume();
            return;
        }

        JPopupMenu menu = createPopupMenu(node, e);
        if (menu == null || menu.getComponentCount() <= 0) return;

        activePopupMenu = menu;

        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) {
                if (activePopupMenu == menu) {
                    activePopupMenu = null;
                }
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) {
                if (activePopupMenu == menu) {
                    activePopupMenu = null;
                }
            }
        });

        menu.show(this, e.getX(), e.getY());
    }

    protected JPopupMenu createPopupMenu(TreeNode<T> node, MouseEvent e) {
        if (node == null) return null;

        JPopupMenu nodeMenu = node.computePopupMenu();
        if (nodeMenu != null) return nodeMenu;

        if (popupMenuProvider != null) {
            try {
                return popupMenuProvider.apply(new TreePopupContext<>(this, node, getSelectedNodes(), e));
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public void onTreeEvent(String eventType, Consumer<EventTree<T>> handler) {
        addEventListener(eventType, ev -> {
            Object v = ev.getValue();
            if (v instanceof EventTree<?>) {
                @SuppressWarnings("unchecked")
                EventTree<T> typed = (EventTree<T>) v;
                handler.accept(typed);
            }
        });
    }

    protected void dispatchTree(String type, TreeNode<T> node, Object oldValue, Object newValue, CheckState check) {
        dispatchTree(type, node, oldValue, newValue, check, null);
    }

    protected void dispatchTree(String type, TreeNode<T> node, Object oldValue, Object newValue, CheckState check, MouseEvent mouseEvent) {
        EventTree<T> payload = new EventTree<>() {
            @Override public TreeNode<T> getNode() { return node; }
            @Override public TreePath getPath() { return node == null ? null : new TreePath(node.getPath()); }
            @Override public List<TreeNode<T>> getSelected() { return getSelectedNodes(); }
            @Override public Object getOldValue() { return oldValue; }
            @Override public Object getNewValue() { return newValue; }
            @Override public CheckState getCheckState() { return check; }
            @Override public MouseEvent getMouseEvent() { return mouseEvent; }
        };
        dispachEvent(type, this, payload);
    }

    protected void dispatchTreeCheck(TreeNode<T> node, CheckState old, CheckState now) {
        dispatchTree(EventTreeView.NODE_CHECK, node, old, now, now);
    }
}
