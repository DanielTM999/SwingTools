package dtm.stools.component.form;

/**
 * Resultado de uma validação, contendo o estado e a mensagem de erro associada.
 */
public record ValidationResult(boolean valid, String message) {

    private static final ValidationResult OK = new ValidationResult(true, null);

    /**
     * Resultado válido, sem mensagem.
     */
    public static ValidationResult ok() {
        return OK;
    }

    /**
     * Resultado inválido com a mensagem informada.
     */
    public static ValidationResult error(String message) {
        return new ValidationResult(false, message != null ? message : "");
    }

    /**
     * Indica se o resultado representa uma falha de validação.
     */
    public boolean isInvalid() {
        return !valid;
    }
}
