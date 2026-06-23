package dtm.stools.component.tree;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public class TreeNodeRenderer extends JPanel implements TreeCellRenderer {

    protected final JPanel checkSlot = new JPanel(new BorderLayout());
    protected final TriStateCheckBox checkBox = new TriStateCheckBox();
    protected final JLabel labelComp = new JLabel();

    protected boolean selected;
    protected boolean hasFocus;
    protected boolean hovered;
    protected boolean showCheckBoxPlaceholder = true;
    protected int checkBoxSlotWidth = 22;

    public TreeNodeRenderer() {
        super(new BorderLayout(0, 0));
        setOpaque(true);

        checkSlot.setOpaque(false);
        checkSlot.setPreferredSize(new Dimension(checkBoxSlotWidth, 20));
        checkSlot.setMinimumSize(new Dimension(checkBoxSlotWidth, 20));

        checkBox.setOpaque(false);
        checkBox.setBorderPainted(false);
        checkBox.setFocusPainted(false);
        checkBox.setContentAreaFilled(false);
        checkBox.setMargin(new Insets(0, 0, 0, 0));
        checkBox.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));

        labelComp.setOpaque(false);
        labelComp.setIconTextGap(4);
        labelComp.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 4));

        checkSlot.add(checkBox, BorderLayout.CENTER);
        add(checkSlot, BorderLayout.WEST);
        add(labelComp, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {

        this.selected = selected;
        this.hasFocus = hasFocus;
        Object hoveredRow = tree.getClientProperty("TreeView.hoveredRow");
        this.hovered = hoveredRow instanceof Integer && ((Integer) hoveredRow) == row;

        if (value instanceof TreeNode<?> node) {
            checkSlot.setVisible(showCheckBoxPlaceholder || node.isCheckable());
            checkBox.setVisible(node.isCheckable());
            if (node.isCheckable()) {
                checkBox.setState(node.getCheckState());
            }
            labelComp.setIcon(resolveIcon(tree, node, expanded, leaf));
            labelComp.setText(buildLabel(node));
            labelComp.setFont(resolveFont(tree, node));
            labelComp.setEnabled(node.isEnabled());
            labelComp.setToolTipText(buildToolTip(node));
            decorate(tree, node, selected, expanded, leaf, row, hasFocus);
        } else {
            checkSlot.setVisible(false);
            checkBox.setVisible(false);
            labelComp.setIcon(null);
            labelComp.setText(value == null ? "" : value.toString());
            labelComp.setEnabled(true);
            labelComp.setToolTipText(null);
            labelComp.setFont(resolveFont(tree, null));
        }

        applyColors(tree, value instanceof TreeNode<?> node ? node : null, selected);
        return this;
    }

    public boolean isShowCheckBoxPlaceholder() {
        return showCheckBoxPlaceholder;
    }

    public void setShowCheckBoxPlaceholder(boolean showCheckBoxPlaceholder) {
        this.showCheckBoxPlaceholder = showCheckBoxPlaceholder;
    }

    public int getCheckBoxSlotWidth() {
        return checkBoxSlotWidth;
    }

    public void setCheckBoxSlotWidth(int checkBoxSlotWidth) {
        this.checkBoxSlotWidth = Math.max(0, checkBoxSlotWidth);
        Dimension size = new Dimension(this.checkBoxSlotWidth, 20);
        checkSlot.setPreferredSize(size);
        checkSlot.setMinimumSize(size);
    }

    protected void applyColors(JTree tree, TreeNode<?> node, boolean selected) {
        Color fg;
        Color bg;
        if (selected) {
            fg = UIManager.getColor("Tree.selectionForeground");
            if (fg == null) fg = Color.WHITE;
            bg = UIManager.getColor("Tree.selectionBackground");
            if (bg == null) bg = new Color(57, 105, 138);
            if (node != null && node.getForeground() != null) fg = node.getForeground();
        } else if (hovered) {
            fg = UIManager.getColor("Tree.textForeground");
            if (fg == null) fg = tree.getForeground();
            if (fg == null) fg = Color.BLACK;
            Color accent = UIManager.getColor("Tree.selectionBackground");
            if (accent == null) accent = new Color(57, 105, 138);
            Color base = tree.getBackground();
            if (base == null) base = Color.WHITE;
            bg = new Color(
                    (accent.getRed() + base.getRed() * 5) / 6,
                    (accent.getGreen() + base.getGreen() * 5) / 6,
                    (accent.getBlue() + base.getBlue() * 5) / 6
            );
            if (node != null && node.getForeground() != null) fg = node.getForeground();
        } else {
            fg = node == null ? null : node.getForeground();
            if (fg == null) fg = UIManager.getColor("Tree.textForeground");
            if (fg == null) fg = UIManager.getColor("Tree.foreground");
            if (fg == null) fg = tree.getForeground();
            if (fg == null) fg = Color.BLACK;
            bg = node == null ? null : node.getBackground();
            if (bg == null) bg = tree.getBackground();
            if (bg == null) bg = UIManager.getColor("Tree.textBackground");
            if (bg == null) bg = Color.WHITE;
        }
        setBackground(bg);
        labelComp.setForeground(fg);
        checkBox.setForeground(fg);
        checkBox.setSelectionBackground(bg);
    }

    protected void decorate(JTree tree, TreeNode<?> node, boolean selected,
                            boolean expanded, boolean leaf, int row, boolean hasFocus) {
    }

    protected String buildLabel(TreeNode<?> node) {
        String label = node.computeLabel();
        if (node.isLoading()) return label + "  (carregando...)";
        return label;
    }

    protected String buildToolTip(TreeNode<?> node) {
        return node.getTooltip();
    }

    protected Icon resolveIcon(JTree tree, TreeNode<?> node, boolean expanded, boolean leaf) {
        Icon custom = node.computeIcon();
        if (custom != null) return custom;
        if (leaf) return UIManager.getIcon("Tree.leafIcon");
        return expanded ? UIManager.getIcon("Tree.openIcon") : UIManager.getIcon("Tree.closedIcon");
    }

    protected Font resolveFont(JTree tree, TreeNode<?> node) {
        if (node != null && node.getFont() != null) return node.getFont();
        Font f = UIManager.getFont("Tree.font");
        return f != null ? f : tree.getFont();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (d != null) d.height = Math.max(d.height, 20);
        return d;
    }

    public static class TriStateCheckBox extends JCheckBox {
        private CheckState state = CheckState.UNCHECKED;
        private Color selectionBackground;

        public void setState(CheckState s) {
            this.state = s == null ? CheckState.UNCHECKED : s;
            setSelected(this.state == CheckState.CHECKED || this.state == CheckState.INDETERMINATE);
            repaint();
        }

        public CheckState getState() {
            return state;
        }

        public void setSelectionBackground(Color selectionBackground) {
            this.selectionBackground = selectionBackground;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 13;
                Insets ins = getInsets();
                int x = ins.left + Math.max(0, (getWidth() - ins.left - ins.right - size) / 2);
                int y = Math.max(0, (getHeight() - size) / 2);

                Color fill = selectionBackground != null ? selectionBackground : getBackground();
                if (fill == null) fill = Color.WHITE;
                Color border = UIManager.getColor("CheckBox.foreground");
                if (border == null) border = new Color(95, 99, 104);

                g2.setColor(fill);
                g2.fillRect(x, y, size, size);
                g2.setColor(border);
                g2.drawRect(x, y, size - 1, size - 1);

                Color fg = UIManager.getColor("CheckBox.foreground");
                if (fg == null) fg = Color.DARK_GRAY;
                g2.setColor(fg);
                if (state == CheckState.CHECKED) {
                    g2.drawLine(x + 3, y + 7, x + 5, y + 9);
                    g2.drawLine(x + 5, y + 9, x + 10, y + 3);
                } else if (state == CheckState.INDETERMINATE) {
                    g2.fillRect(x + 3, y + 6, size - 6, 2);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
