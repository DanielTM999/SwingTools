package dtm.stools.component.form;

import dtm.stools.i18n.I18n;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Regras de validação prontas para os campos de formulário.
 */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");
    private static final Pattern DIGITS = Pattern.compile("\\D");

    private Validators() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Exige um valor não nulo e não vazio.
     */
    public static <T> Validator<T> required() {
        return required(I18n.getText(Validators.class, "required", "Campo obrigatório"));
    }

    /**
     * Exige um valor não nulo e não vazio com mensagem customizada.
     */
    public static <T> Validator<T> required(String message) {
        return value -> isEmpty(value) ? ValidationResult.error(message) : ValidationResult.ok();
    }

    /**
     * Exige um comprimento mínimo de texto.
     */
    public static Validator<String> minLength(int length) {
        return minLength(length, I18n.getText(Validators.class, "minLength",
                "Informe ao menos " + length + " caracteres"));
    }

    /**
     * Exige um comprimento mínimo de texto com mensagem customizada.
     */
    public static Validator<String> minLength(int length, String message) {
        return value -> value != null && value.length() >= length
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    /**
     * Exige um comprimento máximo de texto.
     */
    public static Validator<String> maxLength(int length) {
        return maxLength(length, I18n.getText(Validators.class, "maxLength",
                "Informe no máximo " + length + " caracteres"));
    }

    /**
     * Exige um comprimento máximo de texto com mensagem customizada.
     */
    public static Validator<String> maxLength(int length, String message) {
        return value -> value == null || value.length() <= length
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    /**
     * Exige que o texto case com a expressão regular informada.
     */
    public static Validator<String> pattern(String regex, String message) {
        Pattern compiled = Pattern.compile(regex);
        return value -> value != null && compiled.matcher(value).matches()
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    /**
     * Exige um endereço de e-mail válido.
     */
    public static Validator<String> email() {
        return email(I18n.getText(Validators.class, "email", "E-mail inválido"));
    }

    /**
     * Exige um endereço de e-mail válido com mensagem customizada.
     */
    public static Validator<String> email(String message) {
        return value -> value != null && EMAIL.matcher(value).matches()
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    /**
     * Exige um número dentro do intervalo informado.
     */
    public static Validator<BigDecimal> range(BigDecimal minimum, BigDecimal maximum) {
        return range(minimum, maximum, I18n.getText(Validators.class, "range",
                "Valor fora do intervalo permitido"));
    }

    /**
     * Exige um número dentro do intervalo informado com mensagem customizada.
     */
    public static Validator<BigDecimal> range(BigDecimal minimum, BigDecimal maximum, String message) {
        return value -> {
            if (value == null) {
                return ValidationResult.error(message);
            }
            boolean aboveMinimum = minimum == null || value.compareTo(minimum) >= 0;
            boolean belowMaximum = maximum == null || value.compareTo(maximum) <= 0;
            return aboveMinimum && belowMaximum ? ValidationResult.ok() : ValidationResult.error(message);
        };
    }

    /**
     * Exige um CPF estruturalmente válido.
     */
    public static Validator<String> cpf() {
        return cpf(I18n.getText(Validators.class, "cpf", "CPF inválido"));
    }

    /**
     * Exige um CPF estruturalmente válido com mensagem customizada.
     */
    public static Validator<String> cpf(String message) {
        return value -> isValidCpf(value) ? ValidationResult.ok() : ValidationResult.error(message);
    }

    /**
     * Exige um CNPJ estruturalmente válido.
     */
    public static Validator<String> cnpj() {
        return cnpj(I18n.getText(Validators.class, "cnpj", "CNPJ inválido"));
    }

    /**
     * Exige um CNPJ estruturalmente válido com mensagem customizada.
     */
    public static Validator<String> cnpj(String message) {
        return value -> isValidCnpj(value) ? ValidationResult.ok() : ValidationResult.error(message);
    }

    /**
     * Exige que o valor seja igual ao fornecido pelo suprimento informado.
     */
    public static <T> Validator<T> matches(java.util.function.Supplier<T> other, String message) {
        return value -> java.util.Objects.equals(value, other.get())
                ? ValidationResult.ok()
                : ValidationResult.error(message);
    }

    /**
     * Cria uma regra a partir de um predicado.
     */
    public static <T> Validator<T> of(Predicate<T> predicate, String message) {
        return value -> predicate.test(value) ? ValidationResult.ok() : ValidationResult.error(message);
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            return text.toString().isBlank();
        }
        if (value instanceof Boolean flag) {
            return !flag;
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Object[] array) {
            return array.length == 0;
        }
        return false;
    }

    private static boolean isValidCpf(String value) {
        if (value == null) {
            return false;
        }
        String digits = DIGITS.matcher(value).replaceAll("");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9, 10) && checkDigit(digits, 10, 11);
    }

    private static boolean isValidCnpj(String value) {
        if (value == null) {
            return false;
        }
        String digits = DIGITS.matcher(value).replaceAll("");
        if (digits.length() != 14 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return weightedDigit(digits, firstWeights) == Character.getNumericValue(digits.charAt(12))
                && weightedDigit(digits, secondWeights) == Character.getNumericValue(digits.charAt(13));
    }

    private static boolean checkDigit(String digits, int position, int startWeight) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (startWeight - i);
        }
        int remainder = sum % 11;
        int expected = remainder < 2 ? 0 : 11 - remainder;
        return expected == Character.getNumericValue(digits.charAt(position));
    }

    private static int weightedDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
