package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.feedback.alert.AlertPanel;
import dtm.stools.component.feedback.avatar.AvatarLabel;
import dtm.stools.component.feedback.badge.BadgeLabel;
import dtm.stools.component.feedback.pagination.PaginationPanel;
import dtm.stools.component.feedback.progress.CircularProgress;
import dtm.stools.component.feedback.progress.ProgressBar;
import dtm.stools.component.feedback.steps.StepsPanel;
import dtm.stools.component.feedback.tooltip.ModernTooltip;
import dtm.stools.component.form.FormField;
import dtm.stools.component.form.FormPanel;
import dtm.stools.component.form.Validators;
import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.panels.accordion.AccordionPanel;
import dtm.stools.component.panels.breadcrumb.BreadcrumbBar;
import dtm.stools.component.panels.card.CardPanel;
import dtm.stools.component.panels.card.StatCard;
import dtm.stools.component.panels.divider.DividerPanel;
import dtm.stools.component.panels.emptystate.EmptyStatePanel;
import dtm.stools.component.panels.scroll.ScrollPanel;
import dtm.stools.component.panels.skeleton.SkeletonPanel;
import dtm.stools.component.panels.toolbar.ToolBarPanel;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class ModernPanelsExample {

    private static final JTextArea LOG = new JTextArea(6, 40);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernPanelsExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("Componentes modernos - painéis, feedback e formulário");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 860);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.STRETCH)
                .gap(UiTokens.space(4))
                .padding(UiTokens.space(4))
                .build());

        content.add(buildToolbar(), FlexBoxLayout.FlexConstraints.of().fixedHeight(46));
        content.add(buildBreadcrumb(), FlexBoxLayout.FlexConstraints.of().fixedHeight(26));
        content.add(buildStatCards(), FlexBoxLayout.FlexConstraints.of().fixedHeight(140));
        content.add(buildAlerts(), FlexBoxLayout.FlexConstraints.of().fixedHeight(180));
        content.add(buildFeedbackRow(), FlexBoxLayout.FlexConstraints.of().fixedHeight(120));
        content.add(buildForm(), FlexBoxLayout.FlexConstraints.of().fixedHeight(260));
        content.add(buildAccordion(), FlexBoxLayout.FlexConstraints.of().fixedHeight(220));
        content.add(buildMisc(), FlexBoxLayout.FlexConstraints.of().fixedHeight(240));

        LOG.setEditable(false);
        LOG.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        frame.add(new ScrollPanel(content), BorderLayout.CENTER);
        frame.add(new ScrollPanel(LOG), BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static Component buildToolbar() {
        ToolBarPanel toolbar = new ToolBarPanel();
        toolbar.addAction("Novo", null, () -> log("ToolBar -> Novo"))
                .addAction("Editar", null, () -> log("ToolBar -> Editar"))
                .addSeparator()
                .addAction("Exportar", null, () -> log("ToolBar -> Exportar"))
                .addSpacer()
                .addOverflowAction("Configurações", () -> log("ToolBar -> Configurações"))
                .addOverflowAction("Ajuda", () -> log("ToolBar -> Ajuda"));
        return toolbar;
    }

    private static Component buildBreadcrumb() {
        BreadcrumbBar breadcrumb = new BreadcrumbBar();
        breadcrumb.addCrumb("Início", "home")
                .addCrumb("Cadastros", "list")
                .addCrumb("Clientes", "clients")
                .addCrumb("Daniel Melo", "detail");
        breadcrumb.addEventListener(EventType.SELECT, e -> log("Breadcrumb -> " + e.getValue()));
        return breadcrumb;
    }

    private static Component buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 4, UiTokens.space(4), 0));
        row.setOpaque(false);

        row.add(new StatCard("Receita", "R$ 128k")
                .setDelta("+12,4%", StatCard.Trend.UP)
                .setCaption("vs. mês anterior")
                .setSparkline(List.of(3d, 5d, 4d, 8d, 7d, 11d, 13d)));
        row.add(new StatCard("Churn", "2,1%")
                .setDelta("-0,4 p.p.", StatCard.Trend.DOWN)
                .setCaption("meta 2,5%"));
        row.add(new StatCard("Tickets", "312")
                .setDelta("estável", StatCard.Trend.NEUTRAL));

        CardPanel clickable = new CardPanel("Cartão clicável", "Dispara EventType.ACTION");
        clickable.setClickable(true).setContent(new JLabel("Clique aqui"));
        clickable.addEventListener(EventType.ACTION, e -> log("CardPanel -> clique"));
        row.add(clickable);
        return row;
    }

    private static Component buildAlerts() {
        JPanel column = new JPanel(new GridLayout(4, 1, 0, UiTokens.space(2)));
        column.setOpaque(false);

        for (AlertPanel.Severity severity : AlertPanel.Severity.values()) {
            AlertPanel alert = new AlertPanel(severity,
                    switch (severity) {
                        case INFO -> "Informação";
                        case SUCCESS -> "Salvo com sucesso";
                        case WARNING -> "Atenção";
                        case ERROR -> "Falha ao salvar";
                    },
                    "Mensagem de exemplo para a severidade " + severity.name().toLowerCase() + ".");
            alert.addEventListener(EventType.DISMISS, e -> log("AlertPanel -> dispensado " + e.getValue()));
            column.add(alert);
        }
        return column;
    }

    private static Component buildFeedbackRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.space(6), UiTokens.space(2)));
        row.setOpaque(false);

        ProgressBar bar = new ProgressBar(64);
        bar.setShowLabel(true).setPreferredSize(new Dimension(200, 20));

        ProgressBar loading = new ProgressBar();
        loading.setIndeterminate(true).setTone(ProgressBar.Tone.INFO);
        loading.setPreferredSize(new Dimension(160, 20));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTokens.space(2), 0));
        badges.setOpaque(false);
        badges.add(new BadgeLabel("Ativo", BadgeLabel.Tone.SUCCESS).setShowDot(true));
        badges.add(new BadgeLabel("Pendente", BadgeLabel.Tone.WARNING));
        badges.add(new BadgeLabel("Cancelado", BadgeLabel.Tone.DANGER).setStyle(BadgeLabel.Style.SOLID));
        badges.add(new BadgeLabel("Rascunho").setStyle(BadgeLabel.Style.OUTLINE));

        AvatarLabel avatar = new AvatarLabel("Daniel Melo");
        avatar.setPresence(AvatarLabel.Presence.ONLINE).setRingWidth(2);
        ModernTooltip.install(avatar, "Daniel Melo — online");

        row.add(bar);
        row.add(loading);
        row.add(new CircularProgress(72).setDiameter(56));
        row.add(badges);
        row.add(avatar);
        return row;
    }

    private static Component buildForm() {
        CardPanel card = new CardPanel("Cadastro", "FormPanel com validação em bloco");

        FormPanel form = new FormPanel(2);
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JTextField document = new JTextField();
        SegmentedField<String> plan = new SegmentedField<>();
        plan.addSegment("Básico", "BASIC").addSegment("Pro", "PRO").addSegment("Enterprise", "ENTERPRISE");
        CheckBoxField terms = new CheckBoxField("Aceito os termos de uso");

        form.addField(new FormField("name", "Nome", name).setRequired(true).setHelperText("Nome completo"));
        form.addField("email", "E-mail", email, Validators.<String>required().and(Validators.email()));
        form.addField("document", "CPF", document, Validators.cpf());
        form.addField(new FormField("plan", "Plano", plan));
        form.addField(new FormField("terms", "Termos", terms).setRequired(true));

        JButton submit = new JButton("Enviar");
        submit.addActionListener(e -> log(form.submit()
                ? "FormPanel -> enviado " + form.getValues()
                : "FormPanel -> erros " + form.validateAll()));

        JButton reset = new JButton("Limpar");
        reset.addActionListener(e -> form.reset());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.space(2), 0));
        footer.setOpaque(false);
        footer.add(reset);
        footer.add(submit);

        card.setContent(form).setFooter(footer);
        return card;
    }

    private static Component buildAccordion() {
        AccordionPanel accordion = new AccordionPanel(true);
        accordion.addSection("Dados pessoais", new JLabel("  Nome, documento e data de nascimento"))
                .addSection("Endereço", new JLabel("  Logradouro, cidade e CEP"))
                .addSection("Preferências", new JLabel("  Idioma, tema e notificações"));
        accordion.addEventListener(EventType.CHANGE, e -> log("Accordion -> seção expandida"));
        return accordion;
    }

    private static Component buildMisc() {
        JPanel row = new JPanel(new GridLayout(1, 3, UiTokens.space(4), 0));
        row.setOpaque(false);

        StepsPanel steps = new StepsPanel();
        steps.setSteps(List.of("Carrinho", "Entrega", "Pagamento", "Revisão"));
        steps.setCurrentStep(2, false).setClickable(true);
        steps.addEventListener(EventType.STEP, e -> log("StepsPanel -> etapa " + e.getValue()));

        PaginationPanel pagination = new PaginationPanel(14);
        pagination.addEventListener(EventType.PAGE, e -> log("Pagination -> página " + e.getValue()));

        JPanel left = new JPanel(new BorderLayout(0, UiTokens.space(3)));
        left.setOpaque(false);
        left.add(steps, BorderLayout.NORTH);
        left.add(new DividerPanel("ou"), BorderLayout.CENTER);
        left.add(pagination, BorderLayout.SOUTH);

        row.add(left);
        row.add(new EmptyStatePanel("Nenhum registro", "Cadastre o primeiro item para começar")
                .setDashedBorder(true)
                .setActionButton("Novo registro"));
        row.add(new SkeletonPanel().clearBlocks().addAvatarWithLines(40, 3));
        return row;
    }

    private static void log(String message) {
        LOG.append(message + System.lineSeparator());
        LOG.setCaretPosition(LOG.getDocument().getLength());
    }
}
