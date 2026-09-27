package dtm.stools.component.panels.editor.code.utils;

import java.awt.Dimension;

@FunctionalInterface
public interface LoadingSpinnerFactory {

    LoadingIndicator create(LoadingSpinnerContext context);

    static LoadingSpinnerFactory defaults() {
        return context -> {
            LoadingSpinner spinner = new LoadingSpinner();
            Dimension size = new Dimension(context.size(), context.size());
            spinner.setPreferredSize(size);
            spinner.setMinimumSize(size);
            if (context.color() != null) {
                spinner.setForeground(context.color());
            }
            return spinner;
        };
    }
}
