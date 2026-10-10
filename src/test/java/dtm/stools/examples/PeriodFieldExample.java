package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.form.*;
import dtm.stools.component.inputfields.periodfield.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.time.*;

public final class PeriodFieldExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            PeriodField date = new PeriodField(PeriodMode.DATE).setPresentation(PeriodField.Presentation.CALENDAR);
            PeriodField dateTime = new PeriodField(PeriodMode.DATE_TIME).setRequired(true).setPresentation(PeriodField.Presentation.CALENDAR);
            PeriodField time = new PeriodField(PeriodMode.TIME).setAllowOvernight(true);
            time.setValue(new TimeRange(LocalTime.of(22, 0), LocalTime.of(2, 0), true));
            FormPanel form = new FormPanel().addField("dates", "Datas", date).addField("dateTime", "Data e hora", dateTime).addField("shift", "Turno (pode atravessar meia-noite)", time);
            JLabel status = new JLabel(" "); JButton validate = new JButton("Validar períodos");
            validate.addActionListener(e -> status.setText(form.isFormValid() ? form.getValues().toString() : "Corrija as entradas inválidas"));
            JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            panel.add(form); panel.add(validate, BorderLayout.SOUTH); panel.add(status, BorderLayout.NORTH);
            JFrame frame = new JFrame("PeriodField"); frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
}
