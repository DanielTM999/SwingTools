package dtm.stools.component.panels.editor.code.rename;

public enum RenameStyle {
    INLINE,
    MODERN_DIALOG,
    SIMPLE_DIALOG;

    public RenamePresenter createPresenter() {
        return switch (this) {
            case INLINE -> new InlineRenamePresenter();
            case MODERN_DIALOG -> new ModernDialogRenamePresenter();
            case SIMPLE_DIALOG -> new SimpleDialogRenamePresenter();
        };
    }
}
