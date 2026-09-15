package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.activity.Activity;
import dtm.stools.component.menu.popup.ActionPopupMenu;
import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.enums.TrayEventType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class ActionPopupMenuTrayActivityExample {

    public static void main(String[] args) {
        if (!SystemTray.isSupported()) {
            System.err.println("System Tray nao e suportado neste ambiente.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            new TrayActivity().init();
        });
    }

    private static final class TrayActivity extends Activity {

        private boolean suppressInitialShow = true;

        private TrayActivity() {
            super("ActionPopupMenu no System Tray");
        }

        @Override
        protected void onDrawing() {
            super.onDrawing();

            JLabel description = new JLabel("""
                    <html>
                    <div style='text-align:center'>
                      <h2>Activity aberta pelo System Tray</h2>
                      <p>Feche esta janela para voltar ao tray.</p>
                      <p>Use o botao direito no icone para abri-la novamente.</p>
                    </div>
                    </html>
                    """, SwingConstants.CENTER);

            JButton hideButton = new JButton("Ocultar no tray");
            hideButton.addActionListener(event -> setVisible(false));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
            actions.add(hideButton);

            JPanel content = new JPanel(new BorderLayout(12, 12));
            content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
            content.add(description, BorderLayout.CENTER);
            content.add(actions, BorderLayout.SOUTH);

            setContentPane(content);
            setSize(520, 300);
            setLocationRelativeTo(null);
        }

        @Override
        protected void applySystemTrayConfiguration(SystemTrayConfiguration configuration) {
            configuration.enableSystemTray();
            configuration.setAlwaysVisible(true);
            configuration.setRemoveOnRestore(false);
            configuration.setImageIcon(createTrayImage());
        }

        @Override
        protected void onSystemTrayClick(
                MouseEvent event,
                TrayEventType eventType,
                Activity currentActivity
        ) {
            super.onSystemTrayClick(event, eventType, currentActivity);

            if (
                    eventType != TrayEventType.MOUSE_CLICKED
                            || event.getButton() != MouseEvent.BUTTON3
            ) {
                return;
            }

            ActionPopupMenu.create()
                    .minPopupSize(220, 40)
                    .when(!currentActivity.isVisible(), menu -> menu.item(
                            "Abrir Activity",
                            action -> currentActivity.restoreFromTray()
                    ))
                    .when(currentActivity.isVisible(), menu -> menu.item(
                            "Ocultar no tray",
                            action -> currentActivity.setVisible(false)
                    ))
                    .separator()
                    .item("Sair", action -> exitApplication())
                    .showAt(event);
        }

        @Override
        protected void onClose(WindowEvent event) {
            setVisible(false);
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible && suppressInitialShow) {
                suppressInitialShow = false;
                return;
            }

            super.setVisible(visible);
        }

        private Image createTrayImage() {
            int size = 32;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();

            try {
                graphics.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                graphics.setColor(new Color(0x2563EB));
                graphics.fillRoundRect(2, 2, size - 4, size - 4, 10, 10);
                graphics.setColor(Color.WHITE);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

                FontMetrics metrics = graphics.getFontMetrics();
                String label = "S";
                int x = (size - metrics.stringWidth(label)) / 2;
                int y = (size - metrics.getHeight()) / 2 + metrics.getAscent();
                graphics.drawString(label, x, y);
            } finally {
                graphics.dispose();
            }

            return image;
        }
    }
}
