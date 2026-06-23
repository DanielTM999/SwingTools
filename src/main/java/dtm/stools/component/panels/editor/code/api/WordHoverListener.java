package dtm.stools.component.panels.editor.code.api;

public interface WordHoverListener {

    default void onEnter(WordClickEvent event) {
    }

    default void onExit() {
    }
}
