package dtm.stools.component.panels.editor.code.rename;

import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.SymbolKind;

import java.util.Arrays;
import java.util.List;

public record RenamePreparation(
        Range range,
        String placeholder,
        List<Range> occurrences,
        String rejection,
        List<RenameOption> options,
        RenamePresenter presenter,
        SymbolKind kind,
        boolean localFallback,
        Boolean popupEnabled
) {

    public RenamePreparation {
        occurrences = occurrences == null ? List.of() : List.copyOf(occurrences.stream().filter(r -> r != null && r.start() != null && r.end() != null).toList());
        options = options == null ? List.of() : List.copyOf(options.stream().filter(o -> o != null).toList());
    }

    public RenamePreparation(
            Range range,
            String placeholder,
            List<Range> occurrences,
            String rejection,
            List<RenameOption> options,
            RenamePresenter presenter
    ) {
        this(range, placeholder, occurrences, rejection, options, presenter, null, true, null);
    }

    public RenamePreparation(
            Range range,
            String placeholder,
            List<Range> occurrences,
            String rejection,
            List<RenameOption> options,
            RenamePresenter presenter,
            SymbolKind kind,
            boolean localFallback
    ) {
        this(range, placeholder, occurrences, rejection, options, presenter, kind, localFallback, null);
    }

    public static RenamePreparation of(Range range, String placeholder) {
        return new RenamePreparation(range, placeholder, List.of(), null, List.of(), null, null, true, null);
    }

    public static RenamePreparation rejected(String message) {
        return new RenamePreparation(null, null, List.of(), message == null ? "" : message, List.of(), null, null, true, null);
    }

    public boolean isRejected() {
        return rejection != null;
    }

    public RenamePreparation withRange(Range newRange) {
        return new RenamePreparation(newRange, placeholder, occurrences, rejection, options, presenter, kind, localFallback, popupEnabled);
    }

    public RenamePreparation withPlaceholder(String newPlaceholder) {
        return new RenamePreparation(range, newPlaceholder, occurrences, rejection, options, presenter, kind, localFallback, popupEnabled);
    }

    public RenamePreparation withOccurrences(List<Range> newOccurrences) {
        return new RenamePreparation(range, placeholder, newOccurrences, rejection, options, presenter, kind, localFallback, popupEnabled);
    }

    public RenamePreparation withOptions(List<RenameOption> newOptions) {
        return new RenamePreparation(range, placeholder, occurrences, rejection, newOptions, presenter, kind, localFallback, popupEnabled);
    }

    public RenamePreparation withOptions(RenameOption... newOptions) {
        return withOptions(newOptions == null ? List.of() : Arrays.asList(newOptions));
    }

    public RenamePreparation withPresenter(RenamePresenter newPresenter) {
        return new RenamePreparation(range, placeholder, occurrences, rejection, options, newPresenter, kind, localFallback, popupEnabled);
    }

    public RenamePreparation withKind(SymbolKind newKind) {
        return new RenamePreparation(range, placeholder, occurrences, rejection, options, presenter, newKind, localFallback, popupEnabled);
    }

    public RenamePreparation withLocalFallback(boolean newLocalFallback) {
        return new RenamePreparation(range, placeholder, occurrences, rejection, options, presenter, kind, newLocalFallback, popupEnabled);
    }

    public RenamePreparation withPopupEnabled(Boolean newPopupEnabled) {
        return new RenamePreparation(range, placeholder, occurrences, rejection, options, presenter, kind, localFallback, newPopupEnabled);
    }
}
