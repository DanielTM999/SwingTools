package dtm.stools.component.panels.editor.code.ghost;

/**
 * Define como o ghost text (texto fantasma) e disparado automaticamente no editor.
 */
public enum GhostTextActivationMode {

    /** Dispara apenas enquanto o usuario digita (comportamento padrao das IDEs). */
    TYPING,

    /** Dispara apenas quando o cursor fica parado por um tempo (caret ocioso). */
    CARET_IDLE,

    /** Dispara tanto ao digitar quanto quando o cursor fica ocioso. Valor padrao. */
    BOTH,

    /** Desabilita completamente o ghost text (nenhum disparo, nem manual). */
    DISABLED
}
