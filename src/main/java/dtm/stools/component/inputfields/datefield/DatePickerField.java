package dtm.stools.component.inputfields.datefield;

import dtm.stools.component.events.EventListenerComponent;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DatePickerField extends EventListenerComponent {
    void setEditable(boolean editable);

    /**
     * Retorna a data e hora completas selecionadas.
     *
     * @return O LocalDateTime selecionado, ou null se nada estiver selecionado.
     */
    LocalDateTime getSelectedDateTime();

    /**
     * Define a data e hora do componente.
     *
     * @param dateTime O LocalDateTime para selecionar.
     */
    void setSelectedDateTime(LocalDateTime dateTime);

    /**
     * Retorna apenas a data (LocalDate) selecionada.
     *
     * @return O LocalDate selecionado, ou null se nada estiver selecionado.
     */
    LocalDate getSelectedDate();

    /**
     * Define a data do componente (a hora será definida como 00:00).
     *
     * @param date O LocalDate para selecionar.
     */
    void setSelectedDate(LocalDate date);

    /**
     * Retorna o texto formatado como exibido no campo.
     *
     * @return O texto formatado (ex: "03/11/2025").
     */
    String getFormattedText();

    /**
     * Limpa a seleção do componente.
     */
    void clear();

    /**
     * Retorna a instância do componente Swing (JPanel) para que
     * possa ser adicionado a um layout.
     *
     * @return O componente Swing.
     */
    Component getComponent();

    static DatePickerField createOf(){
        return new DatePickerInputField();
    }

    static DatePickerField createOf(String pattern){
        return createOf(pattern, null, null);
    }

    static DatePickerField createOf(String pattern, Dimension calendarButtonDimension){
        return createOf(pattern, null, calendarButtonDimension);
    }

    static DatePickerField createOf(String pattern, Dimension componentDimension, Dimension calendarButtonDimension){
        return new DatePickerInputField(pattern, componentDimension, calendarButtonDimension);
    }
}
