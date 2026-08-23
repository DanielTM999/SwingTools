package dtm.stools.component.panels.accordion;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agrupa seções colapsáveis, opcionalmente permitindo apenas uma seção expandida por vez.
 */
public class AccordionPanel extends PanelEventListener implements SectionGroup {

    public static final String SECTION_CHANGED = "accordionSectionChanged";

    private final List<SectionPanel> sections = new ArrayList<>();

    private boolean exclusive = true;
    private boolean adjusting;

    public AccordionPanel() {
        this(true);
    }

    public AccordionPanel(boolean exclusive) {
        super(null, false);
        this.exclusive = exclusive;
        setOpaque(false);
        setLayout(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.STRETCH)
                .justify(FlexBoxLayout.Justify.START)
                .gap(UiTokens.space(2))
                .build());
    }

    /**
     * Adiciona uma seção com título e conteúdo.
     */
    public AccordionPanel addSection(String title, JComponent content) {
        return addSection(new SectionPanel(title, content));
    }

    /**
     * Adiciona uma seção já construída.
     */
    public AccordionPanel addSection(SectionPanel section) {
        if (section == null) {
            throw new IllegalArgumentException("section cannot be null");
        }
        section.attachGroup(this);
        sections.add(section);
        add(section);

        if (exclusive && sections.size() > 1) {
            section.setExpanded(false, false);
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Remove todas as seções do grupo.
     */
    public AccordionPanel clearSections() {
        sections.forEach(section -> {
            section.attachGroup(null);
            remove(section);
        });
        sections.clear();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define se apenas uma seção pode permanecer expandida.
     */
    public AccordionPanel setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
        if (exclusive) {
            collapseOthers(firstExpanded());
        }
        return this;
    }

    /**
     * Indica se o grupo está em modo exclusivo.
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Expande a seção do índice informado.
     */
    public AccordionPanel expandSection(int index) {
        if (index < 0 || index >= sections.size()) {
            throw new IllegalArgumentException("invalid section index: " + index);
        }
        sections.get(index).setExpanded(true);
        return this;
    }

    /**
     * Colapsa todas as seções.
     */
    public AccordionPanel collapseAll() {
        adjusting = true;
        try {
            sections.forEach(section -> section.setExpanded(false, false));
        } finally {
            adjusting = false;
        }
        return this;
    }

    /**
     * Seções registradas no grupo.
     */
    public List<SectionPanel> getSections() {
        return List.copyOf(sections);
    }

    @Override
    public void notifyExpanded(SectionPanel section) {
        if (adjusting) {
            return;
        }
        if (exclusive) {
            collapseOthers(section);
        }
        Map<String, Object> props = Map.of("title", section.getTitle(), "expanded", true);
        dispatchEvent(SECTION_CHANGED, this, section, props);
        dispatchEvent(EventType.CHANGE, this, section, props);
    }

    private void collapseOthers(SectionPanel keep) {
        adjusting = true;
        try {
            for (SectionPanel section : sections) {
                if (section != keep) {
                    section.setExpanded(false, false);
                }
            }
        } finally {
            adjusting = false;
        }
    }

    private SectionPanel firstExpanded() {
        return sections.stream().filter(SectionPanel::isExpanded).findFirst().orElse(null);
    }
}
