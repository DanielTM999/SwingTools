package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.component.panels.editor.word.controller.WordReviewController;
import dtm.stools.component.panels.editor.word.model.WordComment;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class WordCommentsPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private final WordReviewController review;
    private final BiFunction<String,String,String> ask;
    private final Consumer<Throwable> errors;
    private final JPanel list = new JPanel();
    private final JCheckBox showResolved = new JCheckBox("Mostrar resolvidos");
    private boolean readOnly;

    public WordCommentsPanel(WordReviewController review, BiFunction<String,String,String> ask, Consumer<Throwable> errors) {
        super(new BorderLayout());
        this.review = review; this.ask = ask; this.errors = errors;
        JLabel title = new JLabel("Comentários"); title.setFont(UiTokens.fontBold()); title.setBorder(BorderFactory.createEmptyBorder(8,10,4,10));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.add(title,BorderLayout.NORTH);
        showResolved.setOpaque(false); showResolved.setBorder(BorderFactory.createEmptyBorder(0,8,6,8)); showResolved.addActionListener(e -> refresh());
        header.add(showResolved,BorderLayout.SOUTH);
        add(header,BorderLayout.NORTH);
        list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS)); list.setOpaque(false);
        JPanel holder = new JPanel(new BorderLayout()); holder.setOpaque(false); holder.add(list,BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll,BorderLayout.CENTER);
        setPreferredSize(new Dimension(260,0));
        getAccessibleContext().setAccessibleName("Painel de comentários");
    }
    public void setCloseAction(Runnable close) {
        JPanel header=(JPanel)((BorderLayout)getLayout()).getLayoutComponent(BorderLayout.NORTH);
        Component old=((BorderLayout)header.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        header.remove(old);header.add(WordNavigationPanel.header("Comentários",close),BorderLayout.NORTH);
    }
    public void setReadOnly(boolean value) { readOnly = value; refresh(); }
    public void refresh() {
        list.removeAll();
        List<WordReviewController.CommentThread> threads = review.threads();
        int shown = 0;
        for (var thread : threads) {
            if (thread.comment().resolved() && !showResolved.isSelected()) continue;
            list.add(card(thread)); list.add(Box.createVerticalStrut(6)); shown++;
        }
        if (shown == 0) { JLabel empty = new JLabel("Nenhum comentário"); empty.setForeground(UiTokens.muted()); empty.setBorder(BorderFactory.createEmptyBorder(8,10,8,10)); list.add(empty); }
        list.revalidate(); list.repaint();
    }
    private JComponent card(WordReviewController.CommentThread thread) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(thread.comment().resolved() ? UiTokens.surfaceAlt() : UiTokens.surface());
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0,6,0,6),BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTokens.border()),BorderFactory.createEmptyBorder(6,8,6,8))));
        JLabel anchor = new JLabel("“" + thread.anchor() + "”"); anchor.setForeground(UiTokens.muted()); anchor.setFont(UiTokens.fontSmall());
        card.add(anchor);
        card.add(entry(thread.comment()));
        for (WordComment reply : thread.replies()) { JComponent r = entry(reply); r.setBorder(BorderFactory.createEmptyBorder(4,12,0,0)); card.add(r); }
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING,4,2)); actions.setOpaque(false);
        actions.add(button("Ir",() -> review.select(thread)));
        if (!readOnly) {
            actions.add(button("Responder",() -> { String text = ask.apply("Responder comentário","Resposta"); if (text != null && !text.isBlank()) review.reply(thread.comment().id(),text); }));
            actions.add(button(thread.comment().resolved() ? "Reabrir" : "Resolver",() -> review.resolve(thread.comment().id(),!thread.comment().resolved())));
            actions.add(button("Excluir",() -> review.deleteComment(thread.comment().id())));
        }
        actions.setAlignmentX(LEFT_ALIGNMENT);
        card.add(actions);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,card.getPreferredSize().height));
        return card;
    }
    private JComponent entry(WordComment c) {
        JLabel label = new JLabel("<html><b>" + escape(c.author()) + "</b> <span style='color:gray'>" + DATE.format(c.date()) + (c.resolved() ? " · resolvido" : "") + "</span><br>" + escape(c.text()).replace("\n","<br>") + "</html>");
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
        return label;
    }
    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text); b.setMargin(new Insets(1,6,1,6)); b.setFont(UiTokens.fontSmall());
        b.addActionListener(e -> { try { action.run(); } catch (RuntimeException ex) { errors.accept(ex); } });
        return b;
    }
    private static String escape(String v) { return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
