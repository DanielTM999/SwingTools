package dtm.stools.component.inputfields.colorpicker;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ColorPickerField extends JPanel {
    @Getter
    private final JTextField textField;
    private final JButton colorPreview;
    private Color currentColor;
    private ColorFormat currentFormat;

    @Setter
    private Consumer<Color> onColorChangeListener;

    @Setter
    private boolean autoUpdate;

    @Setter
    private String colorChooserTitle = "Escolher Cor";

    @Setter
    private String colorPreviewTooltip = "Clique para escolher uma cor";

    private static final Pattern HEX_PATTERN = Pattern.compile("^#?[0-9A-Fa-f]{6}$");
    private static final Pattern RGB_PATTERN = Pattern.compile("^rgb\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\)$");
    private static final Pattern RGBA_PATTERN = Pattern.compile("^rgba\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*[0-9.]+\\s*\\)$");

    public ColorPickerField() {
        this(ColorFormat.HEX, Color.WHITE);
    }

    public ColorPickerField(ColorFormat format) {
        this(format, Color.WHITE);
    }

    public ColorPickerField(ColorFormat format, Color initialColor) {
        this.currentFormat = format;
        this.currentColor = initialColor;
        this.autoUpdate = true;

        setLayout(new BorderLayout(5, 0));
        setBorder(new EmptyBorder(2, 2, 2, 2));

        textField = new JTextField(15);
        textField.setText(formatColor(initialColor, format));

        colorPreview = new JButton();
        colorPreview.setPreferredSize(new Dimension(30, 30));
        colorPreview.setBackground(initialColor);
        colorPreview.setFocusable(false);
        colorPreview.setCursor(new Cursor(Cursor.HAND_CURSOR));
        colorPreview.setToolTipText(colorPreviewTooltip);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rightPanel.add(colorPreview);

        add(textField, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        setupListeners();
        updateLookAndFeel();
    }

    private void setupListeners() {
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (autoUpdate) {
                    updateColorFromText();
                }
            }
        });

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateColorFromText();
                textField.setText(formatColor(currentColor, currentFormat));
            }
        });

        colorPreview.addActionListener(e -> openColorChooser());
    }

    private void openColorChooser() {
        Color selectedColor = JColorChooser.showDialog(
                this,
                colorChooserTitle,
                currentColor
        );

        if (selectedColor != null) {
            setColor(selectedColor);
        }
    }

    private void updateColorFromText() {
        String text = textField.getText().trim();
        Color color = parseColor(text);

        if (color != null) {
            currentColor = color;
            colorPreview.setBackground(color);
            textField.setForeground(UIManager.getColor("TextField.foreground"));

            if (onColorChangeListener != null) {
                onColorChangeListener.accept(color);
            }
        } else {
            textField.setForeground(Color.RED);
        }
    }

    private Color parseColor(String text) {
        if (text == null || text.isEmpty()) return null;

        try {
            if (HEX_PATTERN.matcher(text).matches()) {
                String hex = text.startsWith("#") ? text.substring(1) : text;
                return new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16)
                );
            }

            if (RGB_PATTERN.matcher(text).matches()) {
                String[] parts = text.substring(4, text.length() - 1).split(",");
                return new Color(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            }

            if (RGBA_PATTERN.matcher(text).matches()) {
                String[] parts = text.substring(5, text.length() - 1).split(",");
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                float a = Float.parseFloat(parts[3].trim());
                return new Color(r, g, b, (int)(a * 255));
            }

        } catch (Exception e) {
            // Formato inválido
        }

        return null;
    }

    private String formatColor(Color color, ColorFormat format) {
        if (color == null) return "";

        switch (format) {
            case HEX:
                return String.format("#%02X%02X%02X",
                        color.getRed(), color.getGreen(), color.getBlue());

            case RGB:
                return String.format("rgb(%d, %d, %d)",
                        color.getRed(), color.getGreen(), color.getBlue());

            case RGBA:
                return String.format("rgba(%d, %d, %d, %.2f)",
                        color.getRed(), color.getGreen(), color.getBlue(),
                        color.getAlpha() / 255.0);

            case HSL:
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                return String.format("hsl(%d, %d%%, %d%%)",
                        (int)(hsb[0] * 360), (int)(hsb[1] * 100), (int)(hsb[2] * 100));

            case HSLA:
                float[] hsb2 = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                return String.format("hsla(%d, %d%%, %d%%, %.2f)",
                        (int)(hsb2[0] * 360), (int)(hsb2[1] * 100), (int)(hsb2[2] * 100),
                        color.getAlpha() / 255.0);

            default:
                return String.format("#%02X%02X%02X",
                        color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    public void setColor(Color color) {
        if (color != null) {
            this.currentColor = color;
            this.colorPreview.setBackground(color);
            this.textField.setText(formatColor(color, currentFormat));
            this.textField.setForeground(UIManager.getColor("TextField.foreground"));

            if (onColorChangeListener != null) {
                onColorChangeListener.accept(color);
            }
        }
    }

    public Color getColor() {
        return currentColor;
    }

    public void setColorFormat(ColorFormat format) {
        this.currentFormat = format;
        this.textField.setText(formatColor(currentColor, format));
    }

    public ColorFormat getColorFormat() {
        return currentFormat;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
        updateColorFromText();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        colorPreview.setEnabled(enabled);
    }

    public void setColorPreviewVisible(boolean visible) {
        colorPreview.setVisible(visible);
    }

    public void setPreviewSize(int width, int height) {
        colorPreview.setPreferredSize(new Dimension(width, height));
        revalidate();
    }

    public void setColorFromHex(String hex) {
        Color color = parseColor(hex);
        if (color != null) {
            setColor(color);
        }
    }

    public String getColorAsHex() {
        return formatColor(currentColor, ColorFormat.HEX);
    }

    public String getColorAsRGB() {
        return formatColor(currentColor, ColorFormat.RGB);
    }

    public void updateColorPreviewTooltip() {
        colorPreview.setToolTipText(colorPreviewTooltip);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (textField != null) {
            SwingUtilities.updateComponentTreeUI(textField);
            SwingUtilities.updateComponentTreeUI(colorPreview);
            updateLookAndFeel();
        }
    }

    private void updateLookAndFeel() {
        Color textFieldFg = UIManager.getColor("TextField.foreground");
        Color textFieldBg = UIManager.getColor("TextField.background");
        Color selectionBg = UIManager.getColor("TextField.selectionBackground");
        Color selectionFg = UIManager.getColor("TextField.selectionForeground");
        Color caretColor = UIManager.getColor("TextField.caretForeground");
        Font textFieldFont = UIManager.getFont("TextField.font");

        if (textFieldFg != null) textField.setForeground(textFieldFg);
        if (textFieldBg != null) textField.setBackground(textFieldBg);
        if (selectionBg != null) textField.setSelectionColor(selectionBg);
        if (selectionFg != null) textField.setSelectedTextColor(selectionFg);
        if (caretColor != null) textField.setCaretColor(caretColor);
        if (textFieldFont != null) textField.setFont(textFieldFont);

        Color borderColor = UIManager.getColor("Component.borderColor");
        if (borderColor == null) borderColor = Color.GRAY;
        colorPreview.setBorder(BorderFactory.createLineBorder(borderColor, 1));
    }
}