package dtm.stools.component.panels.datefield;

import dtm.stools.component.inputfields.MaskedTextField;
import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implementação do componente de seleção de data (DatePickerField).
 * (Baseado na classe DatePickerField anterior)
 */
public class DatePickerInputField extends PanelEventListener implements DatePickerField {

    private final String format;
    private final boolean hasTime;
    private final MaskedTextField textField;
    private final JButton calendarButton;

    private JPopupMenu popupMenu;
    private LocalDateTime selectedDateTime;
    private LocalDateTime tempDateTime;
    private YearMonth currentYearMonth;
    private int currentDecadeStart;

    private final String[] monthNames;
    private final String[] weekDays;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel dayViewPanel;
    private JPanel monthViewPanel;
    private JPanel yearViewPanel;

    private JButton prevButton;
    private JButton nextButton;
    private JButton monthViewButton;
    private JButton yearViewButton;

    private JPanel daysGridPanel;
    private JPanel monthsGridPanel;
    private JPanel yearsGridPanel;
    private JLabel yearDecadeLabel;

    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;

    private static final String VIEW_DAYS = "DAYS";
    private static final String VIEW_MONTHS = "MONTHS";
    private static final String VIEW_YEARS = "YEARS";

    public DatePickerInputField() {
        this("dd/MM/yyyy", null, null);
    }

    public DatePickerInputField(String format) {
        this(format, null, null);
    }

    public DatePickerInputField(String format, Dimension componentDimension, Dimension calendarButtonDimension) {
        this.format = format;
        this.hasTime = format.contains("HH") || format.contains("hh");
        this.currentYearMonth = YearMonth.now();
        this.selectedDateTime = null;

        DateFormatSymbols symbols = DateFormatSymbols.getInstance();
        String[] monthNamesTemp = symbols.getMonths();
        if (monthNamesTemp.length > 12) {
            monthNamesTemp = Arrays.copyOf(monthNamesTemp, 12);
        }
        this.monthNames = monthNamesTemp;

        String[] shortWeekdays = symbols.getShortWeekdays();
        this.weekDays = new String[]{
                shortWeekdays[Calendar.SUNDAY],
                shortWeekdays[Calendar.MONDAY],
                shortWeekdays[Calendar.TUESDAY],
                shortWeekdays[Calendar.WEDNESDAY],
                shortWeekdays[Calendar.THURSDAY],
                shortWeekdays[Calendar.FRIDAY],
                shortWeekdays[Calendar.SATURDAY]
        };

        setLayout(new BorderLayout());

        if(componentDimension != null){
            setPreferredSize(componentDimension);
        }

        String mask = convertFormatToMask(format);
        textField = new MaskedTextField(mask);

        int fieldHeight = textField.getPreferredSize().height;
        int fieldWidth = 50;

        calendarButton = new JButton("📅");
        calendarButton.setPreferredSize(Objects.requireNonNullElseGet(calendarButtonDimension, () -> new Dimension(fieldWidth, fieldHeight)));
        calendarButton.setFocusPainted(false);
        calendarButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        add(textField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);

        calendarButton.addActionListener(e -> showCalendarPopup());
    }

    /**
     * Converte o formato de data (ex: "dd/MM/yyyy") para
     * uma máscara de texto (ex: "##/##/####").
     */
    private String convertFormatToMask(String format) {
        return format.replace('d', '#')
                .replace('M', '#')
                .replace('y', '#')
                .replace('H', '#')
                .replace('h', '#')
                .replace('m', '#')
                .replace('s', '#');
    }

    /**
     * Exibe o JPopupMenu do calendário.
     */
    private void showCalendarPopup() {
        if (popupMenu == null) {
            createCalendarPopup();
        }

        tempDateTime = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();
        currentYearMonth = YearMonth.of(tempDateTime.getYear(), tempDateTime.getMonth());

        updateView(VIEW_DAYS);
        updateDayView();

        if (hasTime) {
            updateTimeSpinners();
        }

        popupMenu.show(this, 0, this.getHeight());
    }

    /**
     * Cria todos os componentes do JPopupMenu (executado apenas uma vez).
     */
    private void createCalendarPopup() {
        popupMenu = new JPopupMenu();
        popupMenu.setFocusable(true);
        popupMenu.setBackground(UIManager.getColor("Panel.background"));
        popupMenu.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground"), 1));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(UIManager.getColor("Panel.background"));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(UIManager.getColor("Panel.background"));

        dayViewPanel = createDayViewPanel();
        monthViewPanel = createMonthViewPanel();
        yearViewPanel = createYearViewPanel();

        cardPanel.add(dayViewPanel, VIEW_DAYS);
        cardPanel.add(monthViewPanel, VIEW_MONTHS);
        cardPanel.add(yearViewPanel, VIEW_YEARS);

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIManager.getColor("Panel.background"));

        if (hasTime) {
            bottomPanel.add(createTimePanel(), BorderLayout.NORTH);
        }
        bottomPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        popupMenu.add(mainPanel);
    }

    /**
     * Cria o cabeçalho com botões ◀ Mês Ano ▶
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setBackground(UIManager.getColor("Panel.background"));
        headerPanel.setBorder(new EmptyBorder(5, 5, 10, 5));

        prevButton = createNavButton("◀");
        nextButton = createNavButton("▶");

        JPanel monthYearButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        monthYearButtonPanel.setBackground(UIManager.getColor("Panel.background"));

        Font headerFont = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f);

        monthViewButton = new JButton("Novembro");
        monthViewButton.setFont(headerFont);
        monthViewButton.setFocusPainted(false);
        monthViewButton.setBorderPainted(false);
        monthViewButton.setContentAreaFilled(false);
        monthViewButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        monthViewButton.addActionListener(e -> updateView(VIEW_MONTHS));

        yearViewButton = new JButton("2025");
        yearViewButton.setFont(headerFont);
        yearViewButton.setFocusPainted(false);
        yearViewButton.setBorderPainted(false);
        yearViewButton.setContentAreaFilled(false);
        yearViewButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        yearViewButton.addActionListener(e -> updateView(VIEW_YEARS));

        monthYearButtonPanel.add(monthViewButton);
        monthYearButtonPanel.add(yearViewButton);

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthYearButtonPanel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);

        prevButton.addActionListener(e -> navigate(-1));
        nextButton.addActionListener(e -> navigate(1));

        return headerPanel;
    }

    /**
     * Cria o painel da visão de "Dias" (com dias da semana e a grade 6x7)
     */
    private JPanel createDayViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel weekDaysPanel = new JPanel(new GridLayout(1, 7, 5, 5));
        weekDaysPanel.setBackground(UIManager.getColor("Panel.background"));
        Font weekDayFont = UIManager.getFont("Label.font").deriveFont(Font.BOLD, 11f);

        for (String day : weekDays) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(weekDayFont);
            label.setForeground(UIManager.getColor("Label.disabledForeground"));
            weekDaysPanel.add(label);
        }
        panel.add(weekDaysPanel, BorderLayout.NORTH);

        daysGridPanel = new JPanel(new GridLayout(6, 7, 5, 5));
        daysGridPanel.setBackground(UIManager.getColor("Panel.background"));
        panel.add(daysGridPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Cria o painel da visão de "Meses" (grid 4x3)
     */
    private JPanel createMonthViewPanel() {
        monthsGridPanel = new JPanel(new GridLayout(4, 3, 8, 8));
        monthsGridPanel.setBackground(UIManager.getColor("Panel.background"));
        monthsGridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        Font monthFont = UIManager.getFont("Button.font").deriveFont(Font.BOLD, 13f);

        for (int i = 0; i < 12; i++) {
            final int monthIndex = i;
            JButton monthButton = new JButton(monthNames[i]);
            monthButton.setFont(monthFont);
            monthButton.setFocusPainted(false);
            monthButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            monthButton.addActionListener(e -> {
                currentYearMonth = currentYearMonth.withMonth(monthIndex + 1);
                updateView(VIEW_DAYS);
            });

            addLnfHover(monthButton);
            monthsGridPanel.add(monthButton);
        }
        return monthsGridPanel;
    }

    /**
     * Cria o painel da visão de "Anos" (grid 4x3 com navegação de década)
     */
    private JPanel createYearViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel decadeHeader = new JPanel(new BorderLayout(10, 0));
        decadeHeader.setBackground(UIManager.getColor("Panel.background"));

        JButton prevDecadeButton = createNavButton("◀");
        prevDecadeButton.addActionListener(e -> {
            currentDecadeStart -= 12;
            updateYearView();
        });

        JButton nextDecadeButton = createNavButton("▶");
        nextDecadeButton.addActionListener(e -> {
            currentDecadeStart += 12;
            updateYearView();
        });

        yearDecadeLabel = new JLabel("", SwingConstants.CENTER);
        yearDecadeLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f));

        decadeHeader.add(prevDecadeButton, BorderLayout.WEST);
        decadeHeader.add(yearDecadeLabel, BorderLayout.CENTER);
        decadeHeader.add(nextDecadeButton, BorderLayout.EAST);

        panel.add(decadeHeader, BorderLayout.NORTH);

        yearsGridPanel = new JPanel(new GridLayout(4, 3, 8, 8));
        yearsGridPanel.setBackground(UIManager.getColor("Panel.background"));
        Font yearFont = UIManager.getFont("Button.font").deriveFont(Font.BOLD, 13f);

        for (int i = 0; i < 12; i++) {
            JButton yearButton = new JButton();
            yearButton.setFont(yearFont);
            yearButton.setFocusPainted(false);
            yearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            final int yearIndex = i;
            yearButton.addActionListener(e -> {
                int selectedYear = currentDecadeStart + yearIndex;
                currentYearMonth = currentYearMonth.withYear(selectedYear);
                updateView(VIEW_MONTHS);
            });

            addLnfHover(yearButton);
            yearsGridPanel.add(yearButton);
        }

        panel.add(yearsGridPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Cria o painel de seleção de Hora (Spinners)
     */
    private JPanel createTimePanel() {
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        timePanel.setBackground(UIManager.getColor("Panel.background"));
        timePanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));

        hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

        setSpinnerNumericOnly(hourSpinner);
        setSpinnerNumericOnly(minuteSpinner);

        hourSpinner.setPreferredSize(new Dimension(55, 30));
        minuteSpinner.setPreferredSize(new Dimension(55, 30));

        JLabel timeLabel = new JLabel("Horário:");
        timeLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 12f));

        timePanel.add(timeLabel);
        timePanel.add(hourSpinner);
        timePanel.add(new JLabel(":"));
        timePanel.add(minuteSpinner);

        return timePanel;
    }

    /**
     * Cria o painel de botões do rodapé (Hoje, Cancelar, OK)
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));

        JButton todayButton = new JButton("Hoje");
        todayButton.setFocusPainted(false);
        todayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        todayButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            tempDateTime = now;
            currentYearMonth = YearMonth.of(now.getYear(), now.getMonth());

            updateView(VIEW_DAYS);
            updateDayView();

            if (hasTime) {
                updateTimeSpinners();
            } else {
                setSelectedDateTime(tempDateTime);
                popupMenu.setVisible(false);
            }
        });
        panel.add(todayButton);

        if (hasTime) {
            JButton cancelButton = new JButton("Cancelar");
            cancelButton.setFocusPainted(false);
            cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelButton.addActionListener(e -> popupMenu.setVisible(false));

            JButton okButton = new JButton("OK");
            okButton.setFocusPainted(false);
            okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            okButton.addActionListener(e -> {
                int hour = (Integer) hourSpinner.getValue();
                int minute = (Integer) minuteSpinner.getValue();

                tempDateTime = LocalDateTime.of(
                        tempDateTime.getYear(),
                        tempDateTime.getMonth(),
                        tempDateTime.getDayOfMonth(),
                        hour,
                        minute
                );

                setSelectedDateTime(tempDateTime);
                popupMenu.setVisible(false);
            });

            try {
                JRootPane rootPane = SwingUtilities.getRootPane(this);
                if (rootPane != null) {
                    rootPane.setDefaultButton(okButton);
                }
            } catch (Exception e) {}

            panel.add(cancelButton);
            panel.add(okButton);
        }

        return panel;
    }


    /**
     * Alterna a visualização do CardLayout e atualiza os dados.
     * Ponto central para controlar as 3 visões (Dias, Meses, Anos).
     */
    private void updateView(String view) {
        cardLayout.show(cardPanel, view);

        boolean dayView = view.equals(VIEW_DAYS);
        monthViewButton.setEnabled(dayView);
        yearViewButton.setEnabled(dayView);

        switch (view) {
            case VIEW_DAYS:
                updateDayView();
                break;
            case VIEW_MONTHS:
                updateMonthView();
                break;
            case VIEW_YEARS:
                int currentYear = currentYearMonth.getYear();
                if (currentDecadeStart == 0 || currentYear < currentDecadeStart || currentYear >= currentDecadeStart + 12) {
                    currentDecadeStart = (currentYear / 12) * 12;
                }
                updateYearView();
                break;
        }
    }

    /**
     * Atualiza o cabeçalho e a grade de dias.
     */
    private void updateDayView() {
        monthViewButton.setText(monthNames[currentYearMonth.getMonthValue() - 1]);
        yearViewButton.setText(String.valueOf(currentYearMonth.getYear()));

        daysGridPanel.removeAll();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < dayOfWeek; i++) {
            daysGridPanel.add(new JLabel(""));
        }

        int daysInMonth = currentYearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        Font buttonFont = UIManager.getFont("Button.font").deriveFont(13f);
        Font boldButtonFont = buttonFont.deriveFont(Font.BOLD);
        Color defaultBg = UIManager.getColor("Panel.background");
        Color selectedBg = UIManager.getColor("List.selectionBackground");
        Color selectedFg = UIManager.getColor("List.selectionForeground");
        Color todayFg = UIManager.getColor("Menu.acceleratorForeground");
        Color defaultFg = UIManager.getColor("Label.foreground");
        Color todayBorderColor = todayFg.darker();
        Color selectedBorderColor = selectedBg.brighter();
        if (selectedBorderColor.equals(selectedBg)) {
            selectedBorderColor = selectedFg;
        }
        Border todayBorder = BorderFactory.createLineBorder(todayBorderColor, 1);
        Border selectedBorder = BorderFactory.createLineBorder(selectedBorderColor, 2);
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setFocusPainted(false);
            dayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            dayButton.setFont(buttonFont);
            dayButton.setBackground(defaultBg);
            dayButton.setForeground(defaultFg);
            dayButton.setBorder(emptyBorder);

            if (date.equals(today)) {
                dayButton.setFont(boldButtonFont);
                dayButton.setForeground(todayFg);
                dayButton.setBorder(todayBorder);
            }

            if (tempDateTime != null && date.equals(tempDateTime.toLocalDate())) {
                dayButton.setBackground(selectedBg);
                dayButton.setForeground(selectedFg);
                dayButton.setFont(boldButtonFont);
                dayButton.setBorder(selectedBorder);
            }

            final int selectedDay = day;
            dayButton.addActionListener(e -> {
                int hour = hasTime ? (Integer) hourSpinner.getValue() : 0;
                int minute = hasTime ? (Integer) minuteSpinner.getValue() : 0;

                tempDateTime = LocalDateTime.of(
                        currentYearMonth.getYear(),
                        currentYearMonth.getMonth(),
                        selectedDay,
                        hour,
                        minute
                );

                if (!hasTime) {
                    setSelectedDateTime(tempDateTime);
                    popupMenu.setVisible(false);
                } else {
                    updateDayView();
                }
            });

            addLnfHover(dayButton, selectedBg);
            daysGridPanel.add(dayButton);
        }

        int totalCells = dayOfWeek + daysInMonth;
        for (int i = 0; i < (42 - totalCells); i++) {
            daysGridPanel.add(new JLabel(""));
        }

        daysGridPanel.revalidate();
        daysGridPanel.repaint();
    }

    /**
     * Atualiza a grade de meses (destaca o mês selecionado).
     */
    private void updateMonthView() {
        int currentMonthValue = currentYearMonth.getMonthValue();
        Color selectedBg = UIManager.getColor("List.selectionBackground");
        Color selectedFg = UIManager.getColor("List.selectionForeground");
        Color defaultBg = UIManager.getColor("Button.background");
        Color defaultFg = UIManager.getColor("Button.foreground");

        for (int i = 0; i < 12; i++) {
            JButton button = (JButton) monthsGridPanel.getComponent(i);
            if (i == (currentMonthValue - 1)) {
                button.setBackground(selectedBg);
                button.setForeground(selectedFg);
            } else {
                button.setBackground(defaultBg);
                button.setForeground(defaultFg);
            }
        }
    }

    /**
     * Atualiza a grade de anos (destaca o ano selecionado).
     */
    private void updateYearView() {
        int currentYear = currentYearMonth.getYear();

        yearDecadeLabel.setText(currentDecadeStart + " - " + (currentDecadeStart + 11));

        Color selectedBg = UIManager.getColor("List.selectionBackground");
        Color selectedFg = UIManager.getColor("List.selectionForeground");
        Color defaultBg = UIManager.getColor("Button.background");
        Color defaultFg = UIManager.getColor("Button.foreground");

        for (int i = 0; i < 12; i++) {
            int year = currentDecadeStart + i;
            JButton button = (JButton) yearsGridPanel.getComponent(i);
            button.setText(String.valueOf(year));

            if (year == currentYear) {
                button.setBackground(selectedBg);
                button.setForeground(selectedFg);
            } else {
                button.setBackground(defaultBg);
                button.setForeground(defaultFg);
            }
        }
    }


    /**
     * Adiciona efeito de "hover" (mouse over) que respeita o L&F.
     */
    private void addLnfHover(JButton button) {
        addLnfHover(button, UIManager.getColor("List.selectionBackground"));
    }

    /**
     * Adiciona efeito de "hover" (mouse over) que respeita o L&F.
     * (Versão corrigida para lidar com cores de fundo nulas ou padrão)
     */
    private void addLnfHover(JButton button, Color selectionColor) {
        Color hoverBg = UIManager.getColor("controlHighlight");
        final Color[] originalBg = {null};

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (originalBg[0] == null) {
                    originalBg[0] = button.getBackground();
                }
                if (!button.getBackground().equals(selectionColor)) {
                    button.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                if (button.getBackground().equals(hoverBg)) {
                    button.setBackground(originalBg[0]);
                }
            }
        });
    }

    /**
     * Cria um botão de navegação (seta) padronizado.
     */
    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(UIManager.getColor("Button.background"));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return button;
    }

    /**
     * Navega para frente (1) ou para trás (-1) dependendo da view atual.
     * Este é o handler dos botões ◀ e ▶ principais.
     */
    private void navigate(int amount) {
        String currentView = getCurrentView();
        switch (currentView) {
            case VIEW_DAYS:
                currentYearMonth = currentYearMonth.plusMonths(amount);
                updateDayView();
                break;
            case VIEW_MONTHS:
                currentYearMonth = currentYearMonth.plusYears(amount);
                yearViewButton.setText(String.valueOf(currentYearMonth.getYear()));
                updateMonthView();
                break;
            case VIEW_YEARS:
                currentDecadeStart += (amount * 12);
                updateYearView();
                break;
        }
    }

    /**
     * Descobre qual painel do CardLayout está visível.
     */
    private String getCurrentView() {
        for (Component comp : cardPanel.getComponents()) {
            if (comp.isVisible()) {
                if (comp == dayViewPanel) return VIEW_DAYS;
                if (comp == monthViewPanel) return VIEW_MONTHS;
                if (comp == yearViewPanel) return VIEW_YEARS;
            }
        }
        return VIEW_DAYS;
    }

    /**
     * Impede que letras sejam digitadas nos Spinners de número E formata para 2 dígitos.
     */
    private void setSpinnerNumericOnly(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();

            NumberFormat format = new DecimalFormat("00");
            format.setGroupingUsed(false);
            NumberFormatter formatter = new NumberFormatter(format);

            if (spinner.getModel() instanceof SpinnerNumberModel model) {
                if (model.getMinimum() != null) {
                    formatter.setMinimum(model.getMinimum());
                }
                if (model.getMaximum() != null) {
                    formatter.setMaximum(model.getMaximum());
                }
            }

            formatter.setAllowsInvalid(false);
            formatter.setCommitsOnValidEdit(true);
            tf.setFormatterFactory(new DefaultFormatterFactory(formatter));
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(tf::selectAll);
                }
            });
        }
    }

    /**
     * Atualiza os spinners de hora/minuto com base no 'tempDateTime'.
     */
    private void updateTimeSpinners() {
        if (hasTime && tempDateTime != null) {
            hourSpinner.setValue(tempDateTime.getHour());
            minuteSpinner.setValue(tempDateTime.getMinute());
        }
    }

    /**
     * Atualiza o 'MaskedTextField' principal com o valor de 'selectedDateTime'.
     */
    private void updateTextField() {
        if (selectedDateTime == null) {
            textField.setText("");
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        String formattedDate = selectedDateTime.format(formatter);
        textField.setCleanText(formattedDate.replaceAll("[^0-9]", ""));
    }

    /**
     * Dispara o evento onDataChangeCallback se ele estiver definido.
     */
    private void fireOnDataChange() {
        dispachEvent(EventType.CHANGE, this::getSelectedDateTime);
    }


    @Override
    public void setEditable(boolean editable) {
        textField.setEditable(editable);
    }

    @Override
    public LocalDateTime getSelectedDateTime() {
        return selectedDateTime;
    }

    @Override
    public void setSelectedDateTime(LocalDateTime dateTime) {
        this.selectedDateTime = dateTime;
        this.tempDateTime = dateTime;

        if (dateTime != null) {
            this.currentYearMonth = YearMonth.of(dateTime.getYear(), dateTime.getMonth());
        }

        updateTextField();

        fireOnDataChange();
    }

    @Override
    public LocalDate getSelectedDate() {
        return selectedDateTime != null ? selectedDateTime.toLocalDate() : null;
    }

    @Override
    public void setSelectedDate(LocalDate date) {
        if (date != null) {
            setSelectedDateTime(date.atStartOfDay());
        } else {
            setSelectedDateTime(null);
        }
    }

    @Override
    public String getFormattedText() {
        return textField.getText();
    }

    @Override
    public void clear() {
        this.selectedDateTime = null;
        this.tempDateTime = null;
        this.currentYearMonth = YearMonth.now();
        textField.setText(textField.getText().replaceAll("[0-9]", "_"));

        fireOnDataChange();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void setReadonlyField(boolean readOnly) {
        textField.setReadonly(readOnly);
    }

    @Override
    public boolean isReadonlyField() {
        return textField.isReadonly();
    }

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return;

        if(eventType.equals(EventType.CHANGE)){
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        }else if(eventType.equals(EventType.INPUT)){
            textField.addEventListner(EventType.INPUT, event);
        }
    }
}