package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.provider.*;
import dtm.stools.component.panels.editor.word.ui.popup.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public final class WordPopupController implements AutoCloseable {
    private final DefaultDialogProvider defaultDialog = new DefaultDialogProvider();
    private WordDialogProvider dialog;
    public WordDialogProvider dialogProvider() { return dialog != null ? dialog : defaultDialog; }
    public void setDialog(WordDialogProvider value) { if (value != dialog) defaultDialog.close(); dialog = value; }
    public <T> Optional<T> showDialog(WordDialogRequest<T> request) { return Objects.requireNonNull(dialogProvider().show(request)); }
    private final WordSearchPopupProvider defaultSearch = new DefaultSearchPopupProvider();
    private final WordCommandPaletteProvider defaultPalette = new DefaultCommandPaletteProvider();
    private final WordFileDialogProvider defaultFiles = new DefaultFileDialogProvider();
    private final DefaultConfirmationProvider defaultConfirmation = new DefaultConfirmationProvider();
    private final DefaultObjectPropertiesProvider defaultProperties = new DefaultObjectPropertiesProvider();
    private WordSearchPopupProvider search;
    private WordCommandPaletteProvider palette;
    private WordFileDialogProvider files;
    private WordConfirmationProvider confirmation;
    private WordObjectPropertiesProvider properties;
    private WordPopupHandle searchHandle = WordPopupHandle.closed(), paletteHandle = WordPopupHandle.closed(), propertiesHandle = WordPopupHandle.closed();

    public WordSearchPopupProvider searchProvider() { return search != null ? search : defaultSearch; }
    public WordCommandPaletteProvider paletteProvider() { return palette != null ? palette : defaultPalette; }
    public WordFileDialogProvider fileProvider() { return files != null ? files : defaultFiles; }
    public WordConfirmationProvider confirmationProvider() { return confirmation != null ? confirmation : defaultConfirmation; }
    public WordObjectPropertiesProvider propertiesProvider() { return properties != null ? properties : defaultProperties; }

    public void setSearch(WordSearchPopupProvider value) { if (value != search) { searchHandle.close(); searchHandle = WordPopupHandle.closed(); search = value; } }
    public void setPalette(WordCommandPaletteProvider value) { if (value != palette) { paletteHandle.close(); paletteHandle = WordPopupHandle.closed(); palette = value; } }
    public void setFiles(WordFileDialogProvider value) { files = value; }
    public void setConfirmation(WordConfirmationProvider value) { if(value != confirmation) defaultConfirmation.close(); confirmation = value; }
    public void setProperties(WordObjectPropertiesProvider value) { if (value != properties) { defaultProperties.close(); propertiesHandle.close(); propertiesHandle = WordPopupHandle.closed(); properties = value; } }
    public void reset() { setDialog(null); setSearch(null); setPalette(null); setFiles(null); setConfirmation(null); setProperties(null); }

    public boolean handles(WordProvider provider) {
        return provider instanceof WordDialogProvider || provider instanceof WordSearchPopupProvider || provider instanceof WordCommandPaletteProvider || provider instanceof WordFileDialogProvider
                || provider instanceof WordConfirmationProvider || provider instanceof WordObjectPropertiesProvider;
    }
    public ProviderRegistration register(WordProvider provider) {
        if (provider instanceof WordDialogProvider && dialog != null) throw busy("dialog");
        if (provider instanceof WordSearchPopupProvider && search != null) throw busy("search");
        if (provider instanceof WordCommandPaletteProvider && palette != null) throw busy("command palette");
        if (provider instanceof WordFileDialogProvider && files != null) throw busy("file dialog");
        if (provider instanceof WordConfirmationProvider && confirmation != null) throw busy("confirmation");
        if (provider instanceof WordObjectPropertiesProvider && properties != null) throw busy("object properties");
        if (provider instanceof WordDialogProvider p) setDialog(p);
        if (provider instanceof WordSearchPopupProvider p) setSearch(p);
        if (provider instanceof WordCommandPaletteProvider p) setPalette(p);
        if (provider instanceof WordFileDialogProvider p) setFiles(p);
        if (provider instanceof WordConfirmationProvider p) setConfirmation(p);
        if (provider instanceof WordObjectPropertiesProvider p) setProperties(p);
        return () -> {
            if (dialog == provider) setDialog(null);
            if (search == provider) setSearch(null);
            if (palette == provider) setPalette(null);
            if (files == provider) setFiles(null);
            if (confirmation == provider) setConfirmation(null);
            if (properties == provider) setProperties(null);
        };
    }
    private static IllegalStateException busy(String function) {
        return new IllegalStateException("A custom " + function + " provider is already active; replace it explicitly with the corresponding setter");
    }

    public WordPopupHandle showSearch(Supplier<WordSearchContext> context) {
        if (searchHandle.isOpen()) { searchHandle.toFront(); return searchHandle; }
        searchHandle = Objects.requireNonNullElse(searchProvider().show(context.get()),WordPopupHandle.closed());
        return searchHandle;
    }
    public WordPopupHandle showPalette(Supplier<WordCommandPaletteContext> context) {
        if (paletteHandle.isOpen()) { paletteHandle.toFront(); return paletteHandle; }
        paletteHandle = Objects.requireNonNullElse(paletteProvider().show(context.get()),WordPopupHandle.closed());
        return paletteHandle;
    }
    public WordPopupHandle showProperties(Supplier<WordObjectPropertiesContext> context) {
        if (propertiesHandle.isOpen()) { propertiesHandle.toFront(); return propertiesHandle; }
        propertiesHandle = Objects.requireNonNullElse(propertiesProvider().show(context.get()),WordPopupHandle.closed());
        return propertiesHandle;
    }
    public Optional<Path> chooseFile(WordFileDialogRequest request) { return Objects.requireNonNullElse(fileProvider().choose(request),Optional.empty()); }
    public boolean confirm(WordConfirmationRequest request) { return confirmationProvider().confirm(request); }
    public boolean isSearchOpen() { return searchHandle.isOpen(); }
    public boolean isPaletteOpen() { return paletteHandle.isOpen(); }
    public boolean isPropertiesOpen() { return propertiesHandle.isOpen() || properties == null && defaultProperties.isOpen(); }
    public void dismissTransient() {
        defaultDialog.close();
        defaultConfirmation.close();
        defaultProperties.close();
        searchHandle.close(); paletteHandle.close(); propertiesHandle.close();
        searchHandle = paletteHandle = propertiesHandle = WordPopupHandle.closed();
    }
    @Override public void close() { dismissTransient(); }
}
