package dtm.stools.component.textfields.filepicker;

import dtm.stools.component.textfields.events.EventListenerComponent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public interface FilePickerField extends EventListenerComponent {

    /**
     * Retorna a lista de arquivos (ou pastas) selecionados.
     * @return Uma List<File>. Se nenhum arquivo for selecionado, retorna uma lista vazia.
     */
    List<File> getSelectedFiles();

    /**
     * Define a lista de arquivos (ou pastas) programaticamente.
     * @param files A lista de arquivos a ser definida.
     */
    void setSelectedFiles(List<File> files);

    /**
     * Método de conveniência para obter o *primeiro* arquivo da lista de seleção.
     * @return O primeiro java.io.File selecionado, ou null se a lista estiver vazia.
     */
    File getSelectedFile();

    /**
     * Método de conveniência para definir um *único* arquivo.
     * @param file O arquivo a ser definido.
     */
    void setSelectedFile(File file);

    /**
     * Retorna o texto de exibição do campo (pode ser um caminho ou um resumo).
     * @return Uma String com o texto visível.
     */
    String getDisplayText();

    /**
     * Limpa a seleção do componente.
     */
    void clear();

    /**
     * Retorna a instância do componente Swing (JPanel) para que
     * possa ser adicionado a um layout.
     * @return O componente Swing.
     */
    Component getComponent();


    /**
     * Cria um seletor de Arquivos.
     * @param multiSelectionEnabled true para permitir seleção múltipla, false para única.
     */
    static FilePickerField createForFiles(boolean multiSelectionEnabled) {
        String title = multiSelectionEnabled ? "Selecionar Arquivos" : "Selecionar Arquivo";
        return new FilePickerFieldImple(JFileChooser.FILES_ONLY, title, multiSelectionEnabled);
    }

    /**
     * Cria um seletor de Pastas.
     * @param multiSelectionEnabled true para permitir seleção múltipla, false para única.
     */
    static FilePickerField createForDirectories(boolean multiSelectionEnabled) {
        String title = multiSelectionEnabled ? "Selecionar Pastas" : "Selecionar Pasta";
        return new FilePickerFieldImple(JFileChooser.DIRECTORIES_ONLY, title, multiSelectionEnabled);
    }

    /**
     * Cria um seletor para Arquivos e Pastas.
     * @param multiSelectionEnabled true para permitir seleção múltipla, false para única.
     */
    static FilePickerField createForFilesAndDirectories(boolean multiSelectionEnabled) {
        String title = "Selecionar Itens";
        return new FilePickerFieldImple(JFileChooser.FILES_AND_DIRECTORIES, title, multiSelectionEnabled);
    }

}
