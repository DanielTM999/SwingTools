package dtm.stools.component;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.command.*;
import dtm.stools.component.form.*;
import dtm.stools.component.inputfields.passwordfield.PasswordField;
import dtm.stools.component.inputfields.periodfield.*;
import dtm.stools.component.inputfields.switchfield.SwitchField;
import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.configs.UiTokens;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class NewControlsVisualTest {
    @Test void rendersNewComponentsInBothThemesAndThreeScales() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel(); float originalScale = UiTokens.getScaleFactor();
            try {
                Path directory = Paths.get("target", "new-controls"); Files.createDirectories(directory);
                for (boolean dark : new boolean[]{false, true}) for (float scale : new float[]{1f, 1.5f, 2f}) {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    UiTokens.refresh(); UiTokens.setScaleFactor(scale);
                    JPanel root = new JPanel(new GridLayout(1, 2, UiTokens.space(4), 0));
                    root.setBorder(BorderFactory.createEmptyBorder(UiTokens.space(4), UiTokens.space(4), UiTokens.space(4), UiTokens.space(4)));
                    JPanel left = new JPanel(new BorderLayout(UiTokens.space(2), UiTokens.space(2)));
                    PasswordField password = new PasswordField().setPassword("sample".toCharArray(), false);
                    PeriodField period = new PeriodField(PeriodMode.DATE_TIME).setPresentation(PeriodField.Presentation.CALENDAR);
                    period.setValue(new DateTimeRange(LocalDateTime.of(2026, 10, 10, 9, 0), LocalDateTime.of(2026, 10, 12, 18, 0)), false);
                    FormPanel form = new FormPanel().addField("password", "Senha", password).addField("period", "Período", period);
                    JPanel toggles = new JPanel(); toggles.add(new CheckBoxField("Aceito", true)); toggles.add(new SwitchField(true)); toggles.add(new SliderField()); toggles.add(new StepperField());
                    JPanel top = new JPanel(new BorderLayout()); top.add(form); top.add(toggles, BorderLayout.SOUTH); left.add(top, BorderLayout.NORTH);
                    RangeCalendarPanel calendar = new RangeCalendarPanel(Locale.forLanguageTag("pt-BR")); calendar.setSelection(new DateRange(LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 4))); left.add(calendar, BorderLayout.CENTER);
                    CommandPalette palette = new CommandPalette(() -> List.of(new CommandEntry("save", "Salvar", "Arquivo", "Gravar documento", "Ctrl+S", true), new CommandEntry("disabled", "Indisponível", "Edição", "", "", false)), id -> true);
                    JPanel right = new JPanel(new BorderLayout()); right.add(palette);
                    ClockPickerPanel clock = new ClockPickerPanel(Locale.forLanguageTag("pt-BR")); clock.setTime(LocalTime.of(18, 37));
                    ClockPickerPanel minuteClock = new ClockPickerPanel(Locale.forLanguageTag("pt-BR")); minuteClock.setTime(LocalTime.of(8, 17)); minuteClock.setSelectingHours(false);
                    JPanel clocks = new JPanel(new GridLayout(1, 2, UiTokens.space(2), 0)); clocks.add(clock); clocks.add(minuteClock);
                    right.add(clocks, BorderLayout.SOUTH); root.add(left); root.add(right);
                    Dimension preferred = root.getPreferredSize(); root.setSize(preferred.width, preferred.height); layout(root);
                    assertTrue(period.getHeight() >= period.getPreferredSize().height, "Period input must fit within the form");
                    assertTrue(period.getY() + period.getHeight() <= form.getField("period").getHeight());
                    BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics(); try { root.printAll(g); } finally { g.dispose(); }
                    assertTrue(image.getWidth() > 500); assertTrue(image.getHeight() > 300);
                    ImageIO.write(image, "png", directory.resolve((dark ? "dark" : "light") + "-" + Math.round(scale * 100) + ".png").toFile());
                }
            } catch (Exception e) { throw new AssertionError(e); }
            finally {
                UiTokens.setScaleFactor(originalScale);
                try { UIManager.setLookAndFeel(original); } catch (UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
                UiTokens.refresh();
            }
        });
    }
    private static void layout(Container container) { container.doLayout(); for (Component c : container.getComponents()) if (c instanceof Container child) layout(child); }
}
