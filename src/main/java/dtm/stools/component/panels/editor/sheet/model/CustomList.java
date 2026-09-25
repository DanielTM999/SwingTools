package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;

public record CustomList(List<String> items) {
    public CustomList { items = List.copyOf(items); }

    public static final List<CustomList> DEFAULTS = List.of(
            new CustomList(List.of("dom", "seg", "ter", "qua", "qui", "sex", "sáb")),
            new CustomList(List.of("domingo", "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado")),
            new CustomList(List.of("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")),
            new CustomList(List.of("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")),
            new CustomList(List.of("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")),
            new CustomList(List.of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")),
            new CustomList(List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")),
            new CustomList(List.of("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")));

    public int indexOf(String value) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).equalsIgnoreCase(value.strip())) return i;
        return -1;
    }
}
