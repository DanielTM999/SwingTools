package dtm.stools.context;

import dtm.stools.component.popup.ModernDialog;
import dtm.stools.component.popup.ModernComponentDialog;
import dtm.stools.component.popup.ModernInputDialog;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

public final class Dialogs {

    private Dialogs() {}

    public static int show(String title, String message) {
        return show(null, title, message, ModernDialog.Type.INFO);
    }

    public static int show(Component parent, String title, String message) {
        return show(parent, title, message, ModernDialog.Type.INFO);
    }

    public static int show(String title, String message, ModernDialog.Type type) {
        return show(null, title, message, type);
    }

    public static int show(Component parent, String title, String message, ModernDialog.Type type) {
        return builder()
                .parent(parent)
                .title(title)
                .message(message)
                .type(type)
                .show();
    }

    public static int info(String message) {
        return info(null, "Informacao", message);
    }

    public static int info(String title, String message) {
        return info(null, title, message);
    }

    public static int info(Component parent, String title, String message) {
        return show(parent, title, message, ModernDialog.Type.INFO);
    }

    public static int success(String message) {
        return success(null, "Sucesso", message);
    }

    public static int success(String title, String message) {
        return success(null, title, message);
    }

    public static int success(Component parent, String title, String message) {
        return show(parent, title, message, ModernDialog.Type.SUCCESS);
    }

    public static int error(String message) {
        return error(null, "Erro", message);
    }

    public static int error(String title, String message) {
        return error(null, title, message);
    }

    public static int error(Component parent, String title, String message) {
        return show(parent, title, message, ModernDialog.Type.ERROR);
    }

    public static int error(Throwable error) {
        return error(null, "Erro", error);
    }

    public static int error(String title, Throwable error) {
        return error(null, title, error);
    }

    public static int error(Component parent, String title, Throwable error) {
        Objects.requireNonNull(error, "error");
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return error(parent, title, message);
    }

    public static boolean confirm(String message) {
        return confirm(null, "Confirmar", message);
    }

    public static boolean confirm(String title, String message) {
        return confirm(null, title, message);
    }

    public static boolean confirm(Component parent, String title, String message) {
        int result = builder()
                .parent(parent)
                .title(title)
                .message(message)
                .type(ModernDialog.Type.QUESTION)
                .option("Sim", JOptionPane.YES_OPTION)
                .option("Nao", JOptionPane.NO_OPTION)
                .show();
        return result == JOptionPane.YES_OPTION;
    }

    public static int question(String message, String... options) {
        return question(null, "Pergunta", message, options);
    }

    public static int question(String title, String message, String... options) {
        return question(null, title, message, options);
    }

    public static int question(Component parent, String title, String message, String... options) {
        Builder builder = builder()
                .parent(parent)
                .title(title)
                .message(message)
                .type(ModernDialog.Type.QUESTION);

        if (options == null || options.length == 0) {
            builder.option("Sim", JOptionPane.YES_OPTION)
                    .option("Nao", JOptionPane.NO_OPTION);
        } else {
            for (int i = 0; i < options.length; i++) {
                builder.option(options[i], i);
            }
        }

        return builder.show();
    }

    public static String input(String message) {
        return input(null, "Entrada", message);
    }

    public static String input(String title, String message) {
        return input(null, title, message);
    }

    public static String input(Component parent, String title, String message) {
        return inputBuilder()
                .parent(parent)
                .title(title)
                .message(message)
                .show();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static InputBuilder inputBuilder() {
        return new InputBuilder();
    }

    public static <T> ComponentBuilder<T> componentBuilder(Class<T> resultType) {
        return new ComponentBuilder<>(resultType);
    }

    static <T> T onEdt(Supplier<T> supplier) {
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }

        FutureTask<T> task = new FutureTask<>(supplier::get);
        try {
            SwingUtilities.invokeAndWait(task);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dialog execution was interrupted.", e);
        } catch (InvocationTargetException e) {
            throw rethrow(e.getCause());
        } catch (ExecutionException e) {
            throw rethrow(e.getCause());
        } catch (Exception e) {
            throw rethrow(e);
        }
    }

    private static RuntimeException rethrow(Throwable error) {
        if (error instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (error instanceof Error fatal) {
            throw fatal;
        }
        return new IllegalStateException("Dialog execution failed.", error);
    }

    public static final class Builder {
        private Component parent;
        private String title = "";
        private String message = "";
        private ModernDialog.Type type = ModernDialog.Type.INFO;
        private Color accentColor;
        private boolean draggable;
        private boolean showIcon = true;
        private final java.util.List<Option> options = new java.util.ArrayList<>();

        private Builder() {}

        public Builder parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public Builder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public Builder type(ModernDialog.Type type) {
            this.type = type == null ? ModernDialog.Type.INFO : type;
            return this;
        }

        public Builder accentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder draggable(boolean draggable) {
            this.draggable = draggable;
            return this;
        }

        public Builder showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public Builder option(String text, int value) {
            options.add(new Option(text, value, null, null));
            return this;
        }

        public Builder option(String text, int value, Color color) {
            options.add(new Option(text, value, color, null));
            return this;
        }

        public Builder option(String text, int value, Color color, Color foreground) {
            options.add(new Option(text, value, color, foreground));
            return this;
        }

        public int show() {
            return onEdt(() -> {
                ModernDialog.ModernDialogBuilder modernDialogBuilder = ModernDialog.builder()
                        .title(title)
                        .message(message)
                        .type(type)
                        .draggable(draggable)
                        .showIcon(showIcon);

                if (accentColor != null) {
                    modernDialogBuilder.accentColor(accentColor);
                }

                for (Option option : options) {
                    if (option.color != null && option.foreground != null) {
                        modernDialogBuilder.option(option.text, option.value, option.color, option.foreground);
                    } else if (option.color != null) {
                        modernDialogBuilder.option(option.text, option.value, option.color);
                    } else {
                        modernDialogBuilder.option(option.text, option.value);
                    }
                }

                return modernDialogBuilder.show(parent);
            });
        }
    }

    public static final class InputBuilder {
        private Component parent;
        private String title = "";
        private String message = "";
        private Color accentColor;
        private JComponent inputComponent;
        private String confirmText = "Confirmar";
        private String cancelText = "Cancelar";
        private boolean draggable = true;
        private boolean showIcon = true;

        private InputBuilder() {}

        public InputBuilder parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public InputBuilder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public InputBuilder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public InputBuilder accentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public InputBuilder input(JComponent component) {
            this.inputComponent = component;
            return this;
        }

        public InputBuilder confirmText(String text) {
            this.confirmText = text == null ? "" : text;
            return this;
        }

        public InputBuilder cancelText(String text) {
            this.cancelText = text == null ? "" : text;
            return this;
        }

        public InputBuilder draggable(boolean draggable) {
            this.draggable = draggable;
            return this;
        }

        public InputBuilder showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public String show() {
            return onEdt(() -> {
                ModernInputDialog.ModernInputDialogBuilder modernInputDialogBuilder = ModernInputDialog.builder()
                        .title(title)
                        .message(message)
                        .confirmText(confirmText)
                        .cancelText(cancelText)
                        .draggable(draggable)
                        .showIcon(showIcon);

                if (accentColor != null) {
                    modernInputDialogBuilder.accentColor(accentColor);
                }

                if (inputComponent != null) {
                    modernInputDialogBuilder.input(inputComponent);
                }

                return modernInputDialogBuilder.show(parent);
            });
        }
    }

    public static final class ComponentBuilder<T> {
        private Component parent;
        private final ModernComponentDialog.ModernComponentDialogBuilder<T> modernComponentDialogBuilder;

        private ComponentBuilder(Class<T> resultType) {
            this.modernComponentDialogBuilder = ModernComponentDialog.builder(resultType);
        }

        public ComponentBuilder<T> parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public ComponentBuilder<T> title(String title) {
            modernComponentDialogBuilder.title(title);
            return this;
        }

        public ComponentBuilder<T> message(String message) {
            modernComponentDialogBuilder.message(message);
            return this;
        }

        public ComponentBuilder<T> type(ModernDialog.Type type) {
            modernComponentDialogBuilder.type(type);
            return this;
        }

        public ComponentBuilder<T> accentColor(Color accentColor) {
            modernComponentDialogBuilder.accentColor(accentColor);
            return this;
        }

        public ComponentBuilder<T> draggable(boolean draggable) {
            modernComponentDialogBuilder.draggable(draggable);
            return this;
        }

        public ComponentBuilder<T> showIcon(boolean showIcon) {
            modernComponentDialogBuilder.showIcon(showIcon);
            return this;
        }

        public ComponentBuilder<T> component(JComponent component) {
            modernComponentDialogBuilder.component(component);
            return this;
        }

        public ComponentBuilder<T> content(JComponent component) {
            modernComponentDialogBuilder.content(component);
            return this;
        }

        public ComponentBuilder<T> form(ModernComponentDialog.FormConfigurer configurer) {
            modernComponentDialogBuilder.form(configurer);
            return this;
        }

        public ComponentBuilder<T> result(ModernComponentDialog.ResultProvider<T> provider) {
            modernComponentDialogBuilder.result(provider);
            return this;
        }

        public ComponentBuilder<T> onValidate(ModernComponentDialog.ValidationHandler<T> handler) {
            modernComponentDialogBuilder.onValidate(handler);
            return this;
        }

        public ComponentBuilder<T> validateOnChange() {
            modernComponentDialogBuilder.validateOnChange();
            return this;
        }

        public ComponentBuilder<T> validateOnChange(boolean validate) {
            modernComponentDialogBuilder.validateOnChange(validate);
            return this;
        }

        public ComponentBuilder<T> applyValidationOnChange() {
            modernComponentDialogBuilder.applyValidationOnChange();
            return this;
        }

        public ComponentBuilder<T> applyValidationOnChange(boolean apply) {
            modernComponentDialogBuilder.applyValidationOnChange(apply);
            return this;
        }

        public ComponentBuilder<T> validationDelayMs(int delayMs) {
            modernComponentDialogBuilder.validationDelayMs(delayMs);
            return this;
        }

        public ComponentBuilder<T> disableConfirmWhenInvalid(boolean disable) {
            modernComponentDialogBuilder.disableConfirmWhenInvalid(disable);
            return this;
        }

        public ComponentBuilder<T> validationTrigger(Component component) {
            modernComponentDialogBuilder.validationTrigger(component);
            return this;
        }

        public <C extends Component> ComponentBuilder<T> validationTrigger(
                C component,
                ModernComponentDialog.ValidationTriggerInstaller<C> installer
        ) {
            modernComponentDialogBuilder.validationTrigger(component, installer);
            return this;
        }

        public <C extends Component> ComponentBuilder<T> customValidationTrigger(
                C component,
                ModernComponentDialog.ValidationTriggerInstaller<C> installer
        ) {
            modernComponentDialogBuilder.customValidationTrigger(component, installer);
            return this;
        }

        public ComponentBuilder<T> onSubmit(ModernComponentDialog.SubmitHandler<T> handler) {
            modernComponentDialogBuilder.onSubmit(handler);
            return this;
        }

        public ComponentBuilder<T> closeOnSubmitSuccess(boolean close) {
            modernComponentDialogBuilder.closeOnSubmitSuccess(close);
            return this;
        }

        public ComponentBuilder<T> confirmText(String text) {
            modernComponentDialogBuilder.confirmText(text);
            return this;
        }

        public ComponentBuilder<T> cancelText(String text) {
            modernComponentDialogBuilder.cancelText(text);
            return this;
        }

        public ComponentBuilder<T> showCancelButton(boolean show) {
            modernComponentDialogBuilder.showCancelButton(show);
            return this;
        }

        public ComponentBuilder<T> buttonColor(Color color) {
            modernComponentDialogBuilder.buttonColor(color);
            return this;
        }

        public ComponentBuilder<T> confirmButtonColor(Color color) {
            modernComponentDialogBuilder.confirmButtonColor(color);
            return this;
        }

        public ComponentBuilder<T> confirmButtonForeground(Color color) {
            modernComponentDialogBuilder.confirmButtonForeground(color);
            return this;
        }

        public ComponentBuilder<T> cancelButtonColor(Color color) {
            modernComponentDialogBuilder.cancelButtonColor(color);
            return this;
        }

        public ComponentBuilder<T> cancelButtonForeground(Color color) {
            modernComponentDialogBuilder.cancelButtonForeground(color);
            return this;
        }

        public ComponentBuilder<T> option(String text, T value) {
            modernComponentDialogBuilder.option(text, value);
            return this;
        }

        public ComponentBuilder<T> option(String text, T value, Color color) {
            modernComponentDialogBuilder.option(text, value, color);
            return this;
        }

        public ComponentBuilder<T> option(String text, T value, Color color, Color foreground) {
            modernComponentDialogBuilder.option(text, value, color, foreground);
            return this;
        }

        public ComponentBuilder<T> submitOption(String text) {
            modernComponentDialogBuilder.submitOption(text);
            return this;
        }

        public ComponentBuilder<T> submitOption(String text, ModernComponentDialog.ResultProvider<T> provider) {
            modernComponentDialogBuilder.submitOption(text, provider);
            return this;
        }

        public ComponentBuilder<T> submitOption(String text, ModernComponentDialog.ResultProvider<T> provider, Color color) {
            modernComponentDialogBuilder.submitOption(text, provider, color);
            return this;
        }

        public ComponentBuilder<T> submitOption(
                String text,
                ModernComponentDialog.ResultProvider<T> provider,
                Color color,
                Color foreground
        ) {
            modernComponentDialogBuilder.submitOption(text, provider, color, foreground);
            return this;
        }

        public ComponentBuilder<T> cancelOption(String text) {
            modernComponentDialogBuilder.cancelOption(text);
            return this;
        }

        public T show() {
            return onEdt(() -> modernComponentDialogBuilder.show(parent));
        }
    }

    private record Option(String text, int value, Color color, Color foreground) {}
}
