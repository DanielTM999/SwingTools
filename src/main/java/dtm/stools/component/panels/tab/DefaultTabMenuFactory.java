package dtm.stools.component.panels.tab;

import javax.swing.*;

class DefaultTabMenuFactory {

    JPopupMenu create(TabbedPanel tabs, TabEntry entry) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem activate = new JMenuItem("Abrir");
        activate.addActionListener(e -> tabs.switchTo(entry.getKey()));
        menu.add(activate);

        JMenuItem rename = new JMenuItem("Renomear");
        rename.setEnabled(tabs.isRenameOnDoubleClickEnabled());
        rename.addActionListener(e -> tabs.beginRename(entry.getKey()));
        menu.add(rename);

        JMenuItem pin = new JMenuItem(entry.isPinned() ? "Desafixar" : "Fixar");
        pin.addActionListener(e -> tabs.setPinned(entry.getKey(), !entry.isPinned()));
        menu.add(pin);

        JMenuItem dirty = new JMenuItem(entry.isDirty() ? "Marcar como salvo" : "Marcar como alterado");
        dirty.addActionListener(e -> tabs.setDirty(entry.getKey(), !entry.isDirty()));
        menu.add(dirty);

        menu.addSeparator();

        JMenuItem close = new JMenuItem("Fechar");
        close.setEnabled(entry.isClosable());
        close.addActionListener(e -> tabs.closeTab(entry.getKey()));
        menu.add(close);

        JMenuItem closeOthers = new JMenuItem("Fechar outras");
        closeOthers.addActionListener(e -> tabs.closeOtherTabs(entry.getKey()));
        menu.add(closeOthers);

        JMenuItem closeAll = new JMenuItem("Fechar todas");
        closeAll.addActionListener(e -> tabs.closeAllTabs());
        menu.add(closeAll);

        JMenuItem closeSaved = new JMenuItem("Fechar salvas");
        closeSaved.addActionListener(e -> tabs.closeSavedTabs());
        menu.add(closeSaved);

        return menu;
    }
}
