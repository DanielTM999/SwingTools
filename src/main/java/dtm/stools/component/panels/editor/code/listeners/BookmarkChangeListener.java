package dtm.stools.component.panels.editor.code.listeners;

@FunctionalInterface
public interface BookmarkChangeListener {
    void onBookmarkChanged(int line, boolean added);
}
