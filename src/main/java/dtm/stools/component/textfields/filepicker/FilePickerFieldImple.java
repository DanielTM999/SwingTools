package dtm.stools.component.textfields.filepicker;

import dtm.stools.component.textfields.events.EventComponent;
import dtm.stools.component.textfields.events.EventListenerComponent;
import dtm.stools.component.textfields.events.EventType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implementação do FilePickerField.
 * Suporta seleção única e múltipla de arquivos ou pastas.
 */
public class FilePickerFieldImple extends JPanel implements FilePickerField, EventListenerComponent {

    private final JTextField textField;
    private final JButton browseButton;

    // --- MODIFICADO: Armazena uma lista de arquivos ---
    private List<File> selectedFiles;

    // Configuração do JFileChooser
    private final int fileSelectionMode;
    private final String dialogTitle;
    private final boolean multiSelectionEnabled;

    // Sistema de Eventos
    private final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    public FilePickerFieldImple(int fileSelectionMode, String dialogTitle, boolean multiSelectionEnabled) {
        this.fileSelectionMode = fileSelectionMode;
        this.dialogTitle = dialogTitle;
        this.multiSelectionEnabled = multiSelectionEnabled;
        this.selectedFiles = new ArrayList<>(); // Inicializa a lista

        setLayout(new BorderLayout());

        textField = new JTextField();
        textField.setEditable(false);

        browseButton = new JButton("...");
        int fieldHeight = textField.getPreferredSize().height;
        browseButton.setPreferredSize(new Dimension(fieldHeight, fieldHeight));
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        add(textField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);

        browseButton.addActionListener(this::onBrowse);
    }

    /**
     * Chamado quando o botão "..." é clicado.
     */
    private void onBrowse(ActionEvent originalEvent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setFileSelectionMode(fileSelectionMode);

        // --- MODIFICADO: Habilita/Desabilita multi-seleção ---
        chooser.setMultiSelectionEnabled(multiSelectionEnabled);

        // Abre o seletor no local do primeiro arquivo/pasta
        if (!selectedFiles.isEmpty()) {
            File firstFile = selectedFiles.get(0);
            if (firstFile.isDirectory()) {
                chooser.setCurrentDirectory(firstFile);
            } else {
                chooser.setCurrentDirectory(firstFile.getParentFile());
                // Se for seleção única, pré-seleciona o arquivo
                if (!multiSelectionEnabled) {
                    chooser.setSelectedFile(firstFile);
                }
            }
        }

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            // --- MODIFICADO: Obtém um ou múltiplos arquivos ---
            List<File> newFiles;
            if (multiSelectionEnabled) {
                newFiles = Arrays.asList(chooser.getSelectedFiles());
            } else {
                newFiles = List.of(chooser.getSelectedFile());
            }
            setSelectedFiles(newFiles, originalEvent);
        }
    }

    /**
     * Atualiza o texto do JTextField com base no número de arquivos selecionados.
     */
    private void updateTextFieldText() {
        if (selectedFiles.isEmpty()) {
            textField.setText("");
        } else if (selectedFiles.size() == 1) {
            // Se for só 1, mostra o caminho completo
            textField.setText(selectedFiles.get(0).getAbsolutePath());
        } else {
            // Se for mais de 1, mostra um resumo
            String itemType = (fileSelectionMode == JFileChooser.DIRECTORIES_ONLY) ? "pastas" : "arquivos";
            textField.setText(selectedFiles.size() + " " + itemType + " selecionados");
        }
    }

    /**
     * Método central para definir os arquivos e disparar eventos.
     */
    private void setSelectedFiles(List<File> files, AWTEvent originalEvent) {
        List<File> oldFiles = this.selectedFiles;
        this.selectedFiles = (files == null) ? new ArrayList<>() : new ArrayList<>(files);

        // Atualiza o campo de texto (para mostrar caminho ou resumo)
        updateTextFieldText();

        // Dispara o evento "change" apenas se a lista mudou
        if (!Objects.equals(oldFiles, this.selectedFiles)) {
            dispachEvent(EventType.CHANGE, originalEvent);
        }
    }

    private void dispachEvent(String eventType, AWTEvent originalEvent) {
        List<Consumer<EventComponent>> consumers = listeners.get(eventType);

        if (consumers != null && !consumers.isEmpty()) {
            EventComponent event = new EventComponent() {
                private final long timestamp = System.currentTimeMillis();

                @Override
                public Component getComponent() {
                    return FilePickerFieldImple.this;
                }

                @Override
                public Object getValue() {
                    // --- MODIFICADO: Retorna a lista ---
                    return getSelectedFiles();
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> T tryGetValue() {
                    try {
                        // Tenta fazer o cast do valor (List<File>) para T
                        return (T) getSelectedFiles();
                    } catch (ClassCastException e) {
                        return null;
                    }
                }

                @Override
                public String getEventType() {
                    return eventType;
                }

            };

            SwingUtilities.invokeLater(() -> {
                consumers.forEach(listener -> listener.accept(event));
            });
        }
    }

    // --- Implementação da Interface Pública ---

    @Override
    public List<File> getSelectedFiles() {
        // Retorna uma cópia para evitar modificação externa
        return new ArrayList<>(selectedFiles);
    }

    @Override
    public void setSelectedFiles(List<File> files) {
        setSelectedFiles(files, null);
    }

    @Override
    public File getSelectedFile() {
        return selectedFiles.isEmpty() ? null : selectedFiles.get(0);
    }

    @Override
    public void setSelectedFile(File file) {
        List<File> files = (file == null) ? new ArrayList<>() : List.of(file);
        setSelectedFiles(files, null);
    }

    @Override
    public String getDisplayText() {
        return textField.getText();
    }

    @Override
    public void clear() {
        setSelectedFiles(null, null);
    }

    @Override
    public Component getComponent() {
        return this; // Retorna o próprio JPanel
    }

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if (eventType == null || eventType.isEmpty() || event == null) return;

        if (eventType.equals(EventType.CHANGE)) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        }
    }
}