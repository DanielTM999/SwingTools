package dtm.stools.component.form;

/**
 * Regra de validação aplicada ao valor de um campo de formulário.
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Avalia o valor informado e devolve o resultado da validação.
     */
    ValidationResult validate(T value);

    /**
     * Encadeia outra regra, aplicada apenas quando esta passa.
     */
    default Validator<T> and(Validator<T> next) {
        if (next == null) {
            return this;
        }
        return value -> {
            ValidationResult result = validate(value);
            return result.isInvalid() ? result : next.validate(value);
        };
    }
}
