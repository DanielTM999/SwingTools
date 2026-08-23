package dtm.stools.component.inputfields.checkfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Agrupa opções {@link RadioField} garantindo seleção única e expondo o valor escolhido de forma tipada.
 */
public class RadioGroupField<T> extends PanelEventListener {

    /**
     * Orientação de distribuição das opções.
     */
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    private final List<RadioField<T>> options = new ArrayList<>();

    private Orientation orientation;
    private RadioField<T> current;
    private boolean adjusting;

    public RadioGroupField() {
        this(Orientation.HORIZONTAL);
    }

    public RadioGroupField(Orientation orientation) {
        super(null, false);
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;
        setOpaque(false);
        applyLayout();
    }

    /**
     * Adiciona uma opção com rótulo e valor associados.
     */
    public RadioGroupField<T> addOption(String text, T value) {
        return addOption(new RadioField<>(text, value));
    }

    /**
     * Adiciona uma opção já construída ao grupo.
     */
    public RadioGroupField<T> addOption(RadioField<T> option) {
        if (option == null) {
            throw new IllegalArgumentException("option cannot be null");
        }
        option.attachGroup(this);
        options.add(option);
        add(option);
        if (option.isSelected()) {
            current = option;
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Cria uma opção para cada valor informado usando o provedor de rótulos.
     */
    public RadioGroupField<T> setOptions(List<T> values, Function<T, String> labelProvider) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        Function<T, String> labels = labelProvider != null ? labelProvider : String::valueOf;
        clearOptions();
        for (T value : values) {
            addOption(labels.apply(value), value);
        }
        return this;
    }

    /**
     * Cria uma opção para cada entrada do mapa de rótulo e valor.
     */
    public RadioGroupField<T> setOptions(LinkedHashMap<String, T> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        clearOptions();
        entries.forEach(this::addOption);
        return this;
    }

    /**
     * Remove todas as opções do grupo.
     */
    public RadioGroupField<T> clearOptions() {
        for (RadioField<T> option : options) {
            option.attachGroup(null);
            remove(option);
        }
        options.clear();
        current = null;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Valor da opção marcada, ou {@code null} quando nada está selecionado.
     */
    public T getSelectedValue() {
        return current != null ? current.getValue() : null;
    }

    /**
     * Opção marcada, ou {@code null} quando nada está selecionado.
     */
    public RadioField<T> getSelectedOption() {
        return current;
    }

    /**
     * Marca a opção cujo valor corresponde ao informado, disparando eventos.
     */
    public RadioGroupField<T> setSelectedValue(T value) {
        return setSelectedValue(value, true);
    }

    /**
     * Marca a opção cujo valor corresponde ao informado, opcionalmente sem disparar eventos.
     */
    public RadioGroupField<T> setSelectedValue(T value, boolean fireEvent) {
        for (RadioField<T> option : options) {
            if (java.util.Objects.equals(option.getValue(), value)) {
                option.setSelected(true, fireEvent);
                return this;
            }
        }
        clearSelection(fireEvent);
        return this;
    }

    /**
     * Desmarca todas as opções.
     */
    public RadioGroupField<T> clearSelection(boolean fireEvent) {
        adjusting = true;
        try {
            for (RadioField<T> option : options) {
                option.setSelected(false, false);
            }
        } finally {
            adjusting = false;
        }
        T previous = current != null ? current.getValue() : null;
        current = null;
        if (fireEvent) {
            Map<String, Object> props = new HashMap<>();
            props.put("oldValue", previous);
            props.put("newValue", null);
            dispatchEvent(EventType.CLEAR, this, null, props);
            dispatchEvent(EventType.CHANGE, this, null, props);
        }
        return this;
    }

    /**
     * Define a orientação de distribuição das opções.
     */
    public RadioGroupField<T> setOrientation(Orientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException("orientation cannot be null");
        }
        this.orientation = orientation;
        applyLayout();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Orientação corrente do grupo.
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Opções registradas no grupo.
     */
    public List<RadioField<T>> getOptions() {
        return List.copyOf(options);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component option : getComponents()) {
            option.setEnabled(enabled);
        }
    }

    void notifySelection(RadioField<T> option, boolean fireEvent) {
        if (adjusting) {
            return;
        }
        adjusting = true;
        try {
            for (RadioField<T> other : options) {
                if (other != option) {
                    other.setSelected(false, false);
                }
            }
        } finally {
            adjusting = false;
        }

        T previous = current != null ? current.getValue() : null;
        current = option;

        if (fireEvent) {
            Map<String, Object> props = new HashMap<>();
            props.put("oldValue", previous);
            props.put("newValue", option.getValue());
            dispatchEvent(EventType.CHANGE, this, option.getValue(), props);
            dispatchEvent(EventType.SELECT, this, option.getValue(), props);
        }
    }

    private void applyLayout() {
        setLayout(FlexBoxLayout.builder()
                .direction(orientation == Orientation.HORIZONTAL
                        ? FlexBoxLayout.Direction.ROW
                        : FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.START)
                .justify(FlexBoxLayout.Justify.START)
                .gap(UiTokens.space(orientation == Orientation.HORIZONTAL ? 4 : 2))
                .build());
    }
}
