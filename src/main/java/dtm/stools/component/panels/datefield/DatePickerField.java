package dtm.stools.component.panels.datefield;

import dtm.stools.component.events.EventListenerComponent;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DatePickerField extends EventListenerComponent {
    void setEditable(boolean editable);

    LocalDateTime getSelectedDateTime();

    void setSelectedDateTime(LocalDateTime dateTime);

    LocalDate getSelectedDate();

    void setSelectedDate(LocalDate date);

    String getFormattedText();

    void clear();

    Component getComponent();

    void setReadonlyField(boolean readOnly);

    boolean isReadonlyField();

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
