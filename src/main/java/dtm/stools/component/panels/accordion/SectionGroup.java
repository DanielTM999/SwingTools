package dtm.stools.component.panels.accordion;

/**
 * Contrato usado pelas seções para avisar o grupo quando uma delas é expandida.
 */
interface SectionGroup {

    /**
     * Notifica o grupo de que a seção informada foi expandida.
     */
    void notifyExpanded(SectionPanel section);
}
