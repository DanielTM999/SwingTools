package dtm.stools.component.panels.editor.code.gutter.layer;

import java.util.List;

/**
 * Permite que um layer transfira seu estado transferível (tipicamente os listeners
 * registrados) para um novo layer que o substitui no gutter.
 *
 * <p>Quando {@link dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter#addLayer}
 * troca um layer por outro do mesmo tipo, o estado registrado no layer antigo (por exemplo,
 * o pintor de linha do {@code CodeEditor}) seria perdido. Implementando esta interface, o
 * layer antigo expõe seus objetos transferíveis e o novo os recebe, mantendo o comportamento
 * vivo após a substituição.</p>
 */
public interface TransferableLayer {

    /**
     * Retorna os objetos transferíveis (listeners) atualmente registrados neste layer.
     * A lista é genérica ({@code List<Object>}) para permitir qualquer tipo de listener.
     */
    List<Object> getTransferableListeners();

    /**
     * Recebe os objetos transferíveis de um layer anterior. A implementação deve aceitar
     * apenas os tipos que entende e ignorar os demais, evitando duplicatas.
     *
     * @param listeners objetos vindos do layer substituído; pode ser {@code null} ou vazio.
     */
    void receiveTransferableListeners(List<Object> listeners);
}
