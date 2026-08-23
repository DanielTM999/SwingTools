package dtm.stools.component;

import dtm.stools.component.feedback.alert.AlertPanel;
import dtm.stools.component.feedback.avatar.AvatarLabel;
import dtm.stools.component.feedback.badge.BadgeLabel;
import dtm.stools.component.feedback.pagination.PaginationPanel;
import dtm.stools.component.feedback.progress.CircularProgress;
import dtm.stools.component.feedback.progress.ProgressBar;
import dtm.stools.component.feedback.steps.StepsPanel;
import dtm.stools.component.form.FormField;
import dtm.stools.component.form.FormPanel;
import dtm.stools.component.form.ValidationResult;
import dtm.stools.component.form.Validators;
import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.panels.accordion.AccordionPanel;
import dtm.stools.component.panels.accordion.SectionPanel;
import dtm.stools.component.panels.breadcrumb.BreadcrumbBar;
import dtm.stools.component.panels.card.CardPanel;
import dtm.stools.component.panels.card.StatCard;
import dtm.stools.component.panels.divider.DividerPanel;
import dtm.stools.component.panels.emptystate.EmptyStatePanel;
import dtm.stools.component.panels.skeleton.SkeletonPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernPanelsSmokeTest {

    @Test
    void cardPaintsInEveryVariant() {
        for (CardPanel.Variant variant : CardPanel.Variant.values()) {
            CardPanel card = new CardPanel("Título", "Subtítulo");
            card.setVariant(variant).setContent(new JLabel("conteúdo"));
            paint(card, 260, 160);
        }
    }

    @Test
    void statCardPaintsWithSparkline() {
        StatCard card = new StatCard("Receita", "R$ 128k");
        card.setDelta("+12,4%", StatCard.Trend.UP)
                .setCaption("vs. mês anterior")
                .setSparkline(List.of(3d, 5d, 4d, 8d, 7d, 11d));
        paint(card, 240, 140);
    }

    @Test
    void accordionKeepsSingleSectionExpanded() {
        AccordionPanel accordion = new AccordionPanel(true);
        SectionPanel first = new SectionPanel("Dados", new JLabel("a"));
        SectionPanel second = new SectionPanel("Endereço", new JLabel("b"));
        first.setAnimated(false);
        second.setAnimated(false);

        accordion.addSection(first).addSection(second);
        assertTrue(first.isExpanded());
        assertFalse(second.isExpanded());

        second.setExpanded(true);
        assertTrue(second.isExpanded());
        assertFalse(first.isExpanded());
        paint(accordion, 320, 220);
    }

    @Test
    void panelsPaintWithoutError() {
        paint(new DividerPanel("ou"), 240, 20);
        paint(new EmptyStatePanel("Nada aqui", "Cadastre o primeiro item").setDashedBorder(true), 320, 200);
        paint(new SkeletonPanel().setAnimated(false), 240, 90);
        paint(new BreadcrumbBar().addCrumb("Início", "home").addCrumb("Clientes", "list").addCrumb("Detalhe", "detail"),
                320, 26);
    }

    @Test
    void badgePaintsInEveryToneAndStyle() {
        for (BadgeLabel.Tone tone : BadgeLabel.Tone.values()) {
            for (BadgeLabel.Style style : BadgeLabel.Style.values()) {
                BadgeLabel badge = new BadgeLabel("Ativo", tone).setStyle(style).setShowDot(true);
                paint(badge, badge.getPreferredSize().width, badge.getPreferredSize().height);
            }
        }
    }

    @Test
    void progressBarClampsToMaximum() {
        ProgressBar bar = new ProgressBar();
        bar.setAnimated(false).setShowLabel(true);

        bar.setValue(150);
        assertEquals(100d, bar.getValue());

        bar.setValue(-20);
        assertEquals(0d, bar.getValue());
        paint(bar, 220, 20);

        CircularProgress ring = new CircularProgress(40);
        paint(ring, 64, 64);
    }

    @Test
    void alertDismissHidesAndRestores() {
        AlertPanel alert = new AlertPanel(AlertPanel.Severity.WARNING, "Atenção", "Verifique os dados");
        paint(alert, 380, 76);

        alert.dismiss();
        assertFalse(alert.isVisible());

        alert.restore();
        assertTrue(alert.isVisible());
    }

    @Test
    void stepsAdvanceAndClamp() {
        StepsPanel steps = new StepsPanel();
        steps.setSteps(List.of("Dados", "Endereço", "Revisão"));

        steps.next();
        assertEquals(1, steps.getCurrentStep());

        steps.next();
        steps.next();
        assertEquals(2, steps.getCurrentStep());

        steps.previous();
        assertEquals(1, steps.getCurrentStep());
        paint(steps, 420, 80);
    }

    @Test
    void paginationClampsAndNavigates() {
        PaginationPanel pagination = new PaginationPanel(12);

        pagination.setCurrentPage(50);
        assertEquals(11, pagination.getCurrentPage());

        pagination.setCurrentPage(-3);
        assertEquals(0, pagination.getCurrentPage());

        pagination.nextPage();
        assertEquals(1, pagination.getCurrentPage());
        paint(pagination, 380, 30);
    }

    @Test
    void avatarDerivesInitials() {
        AvatarLabel avatar = new AvatarLabel("Daniel Teixeira Melo");
        assertEquals("DM", avatar.getInitials());

        avatar.setDisplayName("Ana");
        assertEquals("A", avatar.getInitials());

        avatar.setPresence(AvatarLabel.Presence.ONLINE).setRingWidth(2);
        paint(avatar, 44, 44);
    }

    @Test
    void validatorsCoverCommonRules() {
        assertTrue(Validators.required().validate("texto").valid());
        assertFalse(Validators.required().validate("   ").valid());
        assertFalse(Validators.required().validate(List.of()).valid());

        assertTrue(Validators.email().validate("danielmelo@example.com").valid());
        assertFalse(Validators.email().validate("danielmelo@").valid());

        assertTrue(Validators.minLength(3).validate("abc").valid());
        assertFalse(Validators.minLength(4).validate("abc").valid());

        assertTrue(Validators.range(BigDecimal.ONE, BigDecimal.TEN).validate(BigDecimal.valueOf(5)).valid());
        assertFalse(Validators.range(BigDecimal.ONE, BigDecimal.TEN).validate(BigDecimal.valueOf(50)).valid());

        assertTrue(Validators.cpf().validate("529.982.247-25").valid());
        assertFalse(Validators.cpf().validate("111.111.111-11").valid());
        assertTrue(Validators.cnpj().validate("11.222.333/0001-81").valid());
        assertFalse(Validators.cnpj().validate("11.222.333/0001-00").valid());
    }

    @Test
    void validatorsChainStopsAtFirstFailure() {
        ValidationResult result = Validators.<String>required()
                .and(Validators.minLength(5, "curto"))
                .validate("ab");
        assertFalse(result.valid());
        assertEquals("curto", result.message());
    }

    @Test
    void formPanelValidatesAndCollectsValues() {
        FormPanel form = new FormPanel(2);
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        CheckBoxField terms = new CheckBoxField("Aceito");
        terms.setAnimated(false);

        form.addField(new FormField("name", "Nome", name).setRequired(true));
        form.addField("email", "E-mail", email, Validators.email());
        form.addField(new FormField("terms", "Termos", terms).setRequired(true));

        assertEquals(3, form.validateAll().size());
        assertFalse(form.submit());

        name.setText("Daniel");
        email.setText("daniel@example.com");
        terms.setSelected(true);

        assertTrue(form.validateAll().isEmpty());
        assertTrue(form.submit());

        Map<String, Object> values = form.getValues();
        assertEquals("Daniel", values.get("name"));
        assertEquals(Boolean.TRUE, values.get("terms"));

        form.reset();
        assertEquals("", form.getValues().get("name"));
    }

    @Test
    void formPanelWritesValuesBack() {
        FormPanel form = new FormPanel();
        form.addField("name", "Nome", new JTextField());
        form.setValues(Map.of("name", "Melo"));
        assertEquals("Melo", form.getValues().get("name"));
    }

    private static void paint(JComponent component, int width, int height) {
        component.setSize(width, height);
        component.doLayout();

        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            component.paint(g2);
        } finally {
            g2.dispose();
        }
    }
}
