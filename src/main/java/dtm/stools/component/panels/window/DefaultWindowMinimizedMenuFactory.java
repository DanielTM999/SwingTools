package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;

public class DefaultWindowMinimizedMenuFactory implements WindowMinimizedMenuFactory {
    @Override
    public JPopupMenu createMenu(WindowDesktopPanel desktop, WindowPanel window) {
        JPopupMenu menu = createPopupMenu(desktop, window);
        JMenuItem header = createHeaderItem(window);
        if (header != null) {
            menu.add(header);
            menu.addSeparator();
        }
        addWindowActions(menu, desktop, window);
        return menu;
    }

    protected JPopupMenu createPopupMenu(WindowDesktopPanel desktop, WindowPanel window) {
        return new JPopupMenu();
    }

    protected JMenuItem createHeaderItem(WindowPanel window) {
        JMenuItem header = new JMenuItem(window.getTitle(), window.getIcon());
        Font font = header.getFont();
        if (font != null) header.setFont(font.deriveFont(Font.BOLD));
        header.setEnabled(false);
        return header;
    }

    protected void addWindowActions(JPopupMenu menu, WindowDesktopPanel desktop, WindowPanel window) {
        menu.add(createActionItem("Restaurar", WindowMinimizedMenuAction.RESTORE,
                desktop, window, true));
        menu.add(createActionItem("Maximizar", WindowMinimizedMenuAction.MAXIMIZE,
                desktop, window, window.isMaximizable()));
        menu.addSeparator();
        menu.add(createActionItem("Fechar janela", WindowMinimizedMenuAction.CLOSE,
                desktop, window, window.isClosable()));
    }

    protected JMenuItem createActionItem(String text, WindowMinimizedMenuAction action,
                                         WindowDesktopPanel desktop, WindowPanel window,
                                         boolean enabled) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        item.addActionListener(event -> desktop.performMinimizedMenuAction(window, action));
        return item;
    }
}
